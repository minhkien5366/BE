package com.example.cinema.service.impl;

import com.example.cinema.dto.AdminDashboardDTO;
import com.example.cinema.dto.ComboReportResponse;
import com.example.cinema.dto.MovieRatingDTO;
import com.example.cinema.dto.MovieRevenueDTO;
import com.example.cinema.dto.RevenueChartDTO;
import com.example.cinema.entity.Order;
import com.example.cinema.repository.ShowtimeRepository;
import com.example.cinema.repository.TicketRepository;
import com.example.cinema.repository.OrderRepository;
import com.example.cinema.repository.ReviewRepository;
import com.example.cinema.service.ReportService;
import com.example.cinema.repository.OrderDetailRepository;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private TicketRepository ticketRepository;

    // Thuế suất mặc định nếu frontend không gửi lên
    private static final double DEFAULT_VAT_RATE = 0.10; 
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String FONT_FAMILY = "Times New Roman";
    private static final short FONT_SIZE = 13;

    @Override
    public ByteArrayInputStream exportRevenueReport(
            Long cinemaId,
            LocalDateTime start,
            LocalDateTime end,
            Double taxRateInput // 🔥 Bổ sung nhận tham số tỷ lệ thuế từ Controller
    ) throws IOException {

        // Tính toán tỷ lệ thuế động (Ví dụ: Nhận 10 -> quy đổi về 0.1). Nếu null mặc định là 10%
        double currentVatRate = (taxRateInput != null) ? (taxRateInput / 100.0) : DEFAULT_VAT_RATE;
        int displayTaxPercent = (taxRateInput != null) ? taxRateInput.intValue() : 10;

        // 1. Phân luồng lấy danh sách hóa đơn theo quyền Admin / Super-Admin
        List<Order> orders;
        if (cinemaId != null && cinemaId > 0) {
            orders = orderRepository.findByCinemaItemIdAndCreatedAtBetweenAndStatus(cinemaId, start, end, "PAID");
        } else {
            orders = orderRepository.findByCreatedAtBetweenAndStatus(start, end, "PAID");
        }
        if (orders == null) orders = new ArrayList<>();

        // 2. Gọi dữ liệu cho các sheet phụ
        List<MovieRevenueDTO> movies = getMovieRevenue(start, end);
        if (movies == null) movies = new ArrayList<>();

        List<ComboReportResponse> combos = getBestSellingCombos(cinemaId, start, end);
        if (combos == null) combos = new ArrayList<>();

        try (
                Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()
        ) {
            // --- KHỞI TẠO ĐỊNH DẠNG FONT & STYLE ---
            Font baseFont = workbook.createFont();
            baseFont.setFontName(FONT_FAMILY);
            baseFont.setFontHeightInPoints(FONT_SIZE);

            Font headerFont = workbook.createFont();
            headerFont.setFontName(FONT_FAMILY);
            headerFont.setFontHeightInPoints(FONT_SIZE);
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            Font boldFont = workbook.createFont();
            boldFont.setFontName(FONT_FAMILY);
            boldFont.setFontHeightInPoints(FONT_SIZE);
            boldFont.setBold(true);

            // Style tiêu đề chung
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ORANGE.getIndex()); // Nền cam pastel nhẹ
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            setBorders(headerStyle);

            DataFormat format = workbook.createDataFormat();

            // Style dữ liệu dạng tiền tệ
            CellStyle moneyStyle = workbook.createCellStyle();
            moneyStyle.setFont(baseFont);
            moneyStyle.setDataFormat(format.getFormat("#,##0"));
            setBorders(moneyStyle);

            // Style dữ liệu dạng số nguyên
            CellStyle intStyle = workbook.createCellStyle();
            intStyle.setFont(baseFont);
            intStyle.setDataFormat(format.getFormat("#,##0"));
            setBorders(intStyle);

            // Style văn bản thông thường có border
            CellStyle textStyle = workbook.createCellStyle();
            textStyle.setFont(baseFont);
            setBorders(textStyle);

            // Style nhãn in đậm
            CellStyle labelBoldStyle = workbook.createCellStyle();
            labelBoldStyle.setFont(boldFont);
            setBorders(labelBoldStyle);

            // Style số tiền in đậm
            CellStyle moneyBoldStyle = workbook.createCellStyle();
            moneyBoldStyle.setFont(boldFont);
            moneyBoldStyle.setDataFormat(format.getFormat("#,##0"));
            setBorders(moneyBoldStyle);

            /*
             * ==========================================
             * SHEET 1 : CHI TIẾT DOANH THU ĐƠN HÀNG (ĐÃ BIẾN ĐỘNG THEO THUẾ)
             * ==========================================
             */
            Sheet sheet = workbook.createSheet("Bao Cao Doanh Thu");
            Row headerRow = sheet.createRow(0);

            // 🔥 Tự động chèn con số phần trăm thực tế vào tiêu đề cột
            String[] headers = {
                    "Mã Đơn", "Rạp", "Ngày Thanh Toán", 
                    "Tổng Tiền (Gross)", "Thuế VAT (" + displayTaxPercent + "%)", "Doanh Thu Ròng (Net)", 
                    "Phương Thức"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            double totalGross = 0;
            double totalTax = 0;
            double totalNet = 0;

            for (Order order : orders) {
                double gross = order.getTotalAmount() == null ? 0 : order.getTotalAmount();
                // 🔥 Tính thuế dựa trên tỷ lệ động được truyền vào
                double tax = gross * currentVatRate;
                double net = gross - tax;

                totalGross += gross;
                totalTax += tax;
                totalNet += net;

                Row row = sheet.createRow(rowIdx++);
                
                Cell c0 = row.createCell(0); c0.setCellValue(order.getId() != null ? order.getId().toString() : ""); c0.setCellStyle(textStyle);
                Cell c1 = row.createCell(1); c1.setCellValue(order.getCinemaItem() != null ? order.getCinemaItem().getName() : "N/A"); c1.setCellStyle(textStyle);
                Cell c2 = row.createCell(2); c2.setCellValue(order.getCreatedAt() != null ? order.getCreatedAt().format(DATE_FORMATTER) : "N/A"); c2.setCellStyle(textStyle);

                Cell grossCell = row.createCell(3); grossCell.setCellValue(gross); grossCell.setCellStyle(moneyStyle);
                Cell taxCell = row.createCell(4); taxCell.setCellValue(tax); taxCell.setCellStyle(moneyStyle);
                Cell netCell = row.createCell(5); netCell.setCellValue(net); netCell.setCellStyle(moneyStyle);

                Cell c6 = row.createCell(6); c6.setCellValue(order.getPaymentMethod() != null ? order.getPaymentMethod() : "N/A"); c6.setCellStyle(textStyle);
            }

            // Dòng tổng cộng cho Sheet 1
            Row totalRow = sheet.createRow(rowIdx + 1);
            for(int i=0; i<headers.length; i++) { totalRow.createCell(i).setCellStyle(labelBoldStyle); }
            
            totalRow.getCell(2).setCellValue("TỔNG CỘNG");
            totalRow.getCell(3).setCellValue(totalGross); totalRow.getCell(3).setCellStyle(moneyBoldStyle);
            totalRow.getCell(4).setCellValue(totalTax);   totalRow.getCell(4).setCellStyle(moneyBoldStyle);
            totalRow.getCell(5).setCellValue(totalNet);   totalRow.getCell(5).setCellStyle(moneyBoldStyle);

            /*
             * ==========================================
             * SHEET 2 : DOANH THU THEO PHIM
             * ==========================================
             */
            Sheet movieSheet = workbook.createSheet("Doanh Thu Phim");
            Row movieHeader = movieSheet.createRow(0);
            String[] movieHeaders = {"STT", "Tên Phim", "Doanh Thu"};
            
            for (int i = 0; i < movieHeaders.length; i++) {
                Cell cell = movieHeader.createCell(i);
                cell.setCellValue(movieHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            int movieRowIdx = 1;
            for (MovieRevenueDTO m : movies) {
                Row row = movieSheet.createRow(movieRowIdx);
                Cell c0 = row.createCell(0); c0.setCellValue(movieRowIdx); c0.setCellStyle(textStyle);
                Cell c1 = row.createCell(1); c1.setCellValue(m.getMovieName() != null ? m.getMovieName() : "N/A"); c1.setCellStyle(textStyle);
                
                Cell revCell = row.createCell(2);
                revCell.setCellValue(m.getRevenue());
                revCell.setCellStyle(moneyStyle);
                
                movieRowIdx++;
            }

            /*
             * ==========================================
             * SHEET 3 : COMBO BẮP NƯỚC BÁN CHẠY
             * ==========================================
             */
            Sheet comboSheet = workbook.createSheet("Combo Ban Chay");
            Row comboHeader = comboSheet.createRow(0);
            String[] comboHeaders = {"STT", "Tên Combo", "Số Lượng Bán", "Doanh Thu"};
            
            for (int i = 0; i < comboHeaders.length; i++) {
                Cell cell = comboHeader.createCell(i);
                cell.setCellValue(comboHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            int comboRowIdx = 1;
            for (ComboReportResponse c : combos) {
                Row row = comboSheet.createRow(comboRowIdx);
                Cell c0 = row.createCell(0); c0.setCellValue(comboRowIdx); c0.setCellStyle(textStyle);
                Cell c1 = row.createCell(1); c1.setCellValue(c.getComboName() != null ? c.getComboName() : "N/A"); c1.setCellStyle(textStyle);
                
                Cell qtyCell = row.createCell(2);
                qtyCell.setCellValue(c.getTotalQuantity() != null ? c.getTotalQuantity() : 0);
                qtyCell.setCellStyle(intStyle);
                
                Cell revCell = row.createCell(3);
                revCell.setCellValue(c.getTotalRevenue() != null ? c.getTotalRevenue() : 0.0);
                revCell.setCellStyle(moneyStyle);
                
                comboRowIdx++;
            }

            /*
             * ==========================================
             * SHEET 4 : TỔNG HỢP DASHBOARD THỐNG KÊ
             * ==========================================
             */
            Sheet summarySheet = workbook.createSheet("Tong Hop");
            AdminDashboardDTO dashboard = getAdminDashboard(cinemaId);

            double totalComboRevenue = combos.stream()
                    .filter(c -> c.getTotalRevenue() != null)
                    .mapToDouble(ComboReportResponse::getTotalRevenue)
                    .sum();

            String topMovie = movies.isEmpty() ? "Không có dữ liệu phim" : movies.get(0).getMovieName();
            double topMovieRevenue = movies.isEmpty() ? 0.0 : movies.get(0).getRevenue();

            String topCombo = combos.isEmpty() ? "Không có dữ liệu combo" : combos.get(0).getComboName();
            double topComboRevenue = combos.isEmpty() ? 0.0 : (combos.get(0).getTotalRevenue() != null ? combos.get(0).getTotalRevenue() : 0.0);

            int summaryRowIdx = 0;
            Row sHeaderRow = summarySheet.createRow(summaryRowIdx++);
            
            Cell hCell0 = sHeaderRow.createCell(0); hCell0.setCellValue("Hạng Mục Thống Kê"); hCell0.setCellStyle(headerStyle);
            Cell hCell1 = sHeaderRow.createCell(1); hCell1.setCellValue("Thông Tin / Giá Trị"); hCell1.setCellStyle(headerStyle);
            Cell hCell2 = sHeaderRow.createCell(2); hCell2.setCellValue("Doanh Thu Chi Tiết"); hCell2.setCellStyle(headerStyle);

            Row r1 = summarySheet.createRow(summaryRowIdx++);
            Cell c1_0 = r1.createCell(0); c1_0.setCellValue("Tổng doanh thu hệ thống (Hôm nay)"); c1_0.setCellStyle(labelBoldStyle);
            Cell c1_1 = r1.createCell(1); c1_1.setCellValue(dashboard.todayRevenue); c1_1.setCellStyle(moneyStyle);
            Cell c1_2 = r1.createCell(2); c1_2.setCellValue("-"); c1_2.setCellStyle(textStyle);

            Row r2 = summarySheet.createRow(summaryRowIdx++);
            Cell c2_0 = r2.createCell(0); c2_0.setCellValue("Tổng số vé bán ra (Hôm nay)"); c2_0.setCellStyle(labelBoldStyle);
            Cell c2_1 = r2.createCell(1); c2_1.setCellValue(dashboard.todayTickets); c2_1.setCellStyle(intStyle);
            Cell c2_2 = r2.createCell(2); c2_2.setCellValue("-"); c2_2.setCellStyle(textStyle);

            Row r3 = summarySheet.createRow(summaryRowIdx++);
            Cell c3_0 = r3.createCell(0); c3_0.setCellValue("Tổng số suất chiếu (Hôm nay)"); c3_0.setCellStyle(labelBoldStyle);
            Cell c3_1 = r3.createCell(1); c3_1.setCellValue(dashboard.todayShowtimes); c3_1.setCellStyle(intStyle);
            Cell c3_2 = r3.createCell(2); c3_2.setCellValue("-"); c3_2.setCellStyle(textStyle);

            Row r4 = summarySheet.createRow(summaryRowIdx++);
            Cell c4_0 = r4.createCell(0); c4_0.setCellValue("Tỷ lệ lấp đầy rạp"); c4_0.setCellStyle(labelBoldStyle);
            Cell c4_1 = r4.createCell(1); c4_1.setCellValue(String.format("%.2f%%", dashboard.occupancy)); c4_1.setCellStyle(textStyle);
            Cell c4_2 = r4.createCell(2); c4_2.setCellValue("-"); c4_2.setCellStyle(textStyle);

            Row r5 = summarySheet.createRow(summaryRowIdx++);
            Cell c5_0 = r5.createCell(0); c5_0.setCellValue("Doanh thu Combo thực tế (Theo bộ lọc)"); c5_0.setCellStyle(labelBoldStyle);
            Cell c5_1 = r5.createCell(1); c5_1.setCellValue(totalComboRevenue); c5_1.setCellStyle(moneyStyle);
            Cell c5_2 = r5.createCell(2); c5_2.setCellValue("-"); c5_2.setCellStyle(textStyle);

            Row r6 = summarySheet.createRow(summaryRowIdx++);
            Cell c6_0 = r6.createCell(0); c6_0.setCellValue("Phim doanh thu đỉnh nhất (Theo bộ lọc)"); c6_0.setCellStyle(labelBoldStyle);
            Cell c6_1 = r6.createCell(1); c6_1.setCellValue(topMovie); c6_1.setCellStyle(textStyle);
            Cell c6_2 = r6.createCell(2); c6_2.setCellValue(topMovieRevenue); c6_2.setCellStyle(moneyStyle);

            Row r7 = summarySheet.createRow(summaryRowIdx++);
            Cell c7_0 = r7.createCell(0); c7_0.setCellValue("Combo bán chạy nhất (Theo bộ lọc)"); c7_0.setCellStyle(labelBoldStyle);
            Cell c7_1 = r7.createCell(1); c7_1.setCellValue(topCombo); c7_1.setCellStyle(textStyle);
            Cell c7_2 = r7.createCell(2); c7_2.setCellValue(topComboRevenue); c7_2.setCellStyle(moneyStyle);

            // --- TỰ ĐỘNG CÂN CHỈNH ĐỘ RỘNG CHO TẤT CẢ CÁC SHEETS ---
            for (int i = 0; i < headers.length; i++) { sheet.autoSizeColumn(i); }
            for (int i = 0; i < movieHeaders.length; i++) { movieSheet.autoSizeColumn(i); }
            for (int i = 0; i < comboHeaders.length; i++) { comboSheet.autoSizeColumn(i); }
            summarySheet.autoSizeColumn(0);
            summarySheet.autoSizeColumn(1);
            summarySheet.autoSizeColumn(2);

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    private void setBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    @Override
    public List<ComboReportResponse> getBestSellingCombos(Long cinemaId, LocalDateTime start, LocalDateTime end) {
        if (cinemaId != null && cinemaId > 0) {
            return orderDetailRepository.getBestSellingCombos(cinemaId, start, end);
        } else {
            return orderDetailRepository.getAllBestSellingCombos(start, end);
        }
    }

    @Override
    public List<Map<String, Object>> getCinemaRankingData(LocalDateTime start, LocalDateTime end) {
        return orderRepository.getCinemaRanking(start, end)
                .stream()
                .map(obj -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", obj[0]);
                    map.put("revenue", obj[1]);
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<MovieRatingDTO> getMovieStatistics() {
        return reviewRepository.getTopRatedMovies()
                .stream()
                .map(obj -> new MovieRatingDTO(
                        (String) obj[0],
                        ((Number) obj[1]).doubleValue(),
                        ((Number) obj[2]).longValue()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public AdminDashboardDTO getAdminDashboard(Long cinemaId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();

        Double revenue;
        Long tickets;
        Long showtimes;

        if (cinemaId != null && cinemaId > 0) {
            revenue = orderRepository.getTodayRevenueByCinema(cinemaId, startOfDay);
            tickets = orderRepository.countTodayTicketsByCinema(cinemaId, startOfDay);
            showtimes = showtimeRepository.countTodayShowtimesByCinema(cinemaId);
        } else {
            revenue = orderRepository.getTodayRevenue(startOfDay);
            tickets = orderRepository.countTodayTickets(startOfDay);
            showtimes = showtimeRepository.countTodayShowtimes();
        }

        revenue = revenue == null ? 0.0 : revenue;
        tickets = tickets == null ? 0L : tickets;
        showtimes = showtimes == null ? 0L : showtimes;

        double occupancy = showtimes > 0 ? (tickets * 100.0) / (showtimes * 100.0) : 0.0;

        return new AdminDashboardDTO(
                revenue,
                tickets,
                showtimes,
                Math.min(100.0, occupancy)
        );
    }

    @Override
    public List<RevenueChartDTO> getAdminRevenue7Days(Long cinemaId) {
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        return orderRepository.revenue7DaysByCinema(cinemaId, start)
                .stream()
                .map(o -> new RevenueChartDTO(o[0].toString(), ((Number) o[1]).doubleValue()))
                .collect(Collectors.toList());
    }

    @Override
    public List<MovieRevenueDTO> getMovieRevenue(LocalDateTime startDate, LocalDateTime endDate) {
        return ticketRepository.getMovieRevenue(startDate, endDate)
                .stream()
                .map(row -> new MovieRevenueDTO(
                        (String) row[0],
                        ((Number) row[1]).doubleValue()
                ))
                .toList();
    }
}