package com.example.cinema.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {
    
    @NotNull(message = "Mã suất chiếu (showtimeId) không được để trống!")
    @JsonProperty("showtimeId")
    @JsonAlias({"id", "showtime_id", "stId", "infoId"})
    private Long showtimeId; 
    
    @NotEmpty(message = "Vui lòng chọn ít nhất một ghế ngồi!")
    @JsonProperty("seatIds")
    private List<Long> seatIds;
    
    @JsonProperty("combos")
    private List<ComboOrderDTO> combos;
    
    @JsonProperty("totalAmount")
    private Double totalAmount; 
    
    @NotBlank(message = "Vui lòng chọn phương thức thanh toán!")
    @JsonProperty("paymentMethod")
    private String paymentMethod;

    // --- BỔ SUNG TRƯỜNG NÀY ĐỂ HẾT LỖI getVoucherCode() ---
    @JsonProperty("voucherCode")
    private String voucherCode;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComboOrderDTO {
        @NotNull(message = "Mã combo không được để trống!")
        @JsonProperty("comboId")
        private Long comboId;

        @Min(value = 1, message = "Số lượng combo tối thiểu là 1")
        @JsonProperty("quantity")
        private Integer quantity;
    }
}