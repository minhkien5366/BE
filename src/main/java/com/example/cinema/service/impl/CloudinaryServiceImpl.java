package com.example.cinema.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.cinema.service.CloudinaryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    // Khai báo Constructor: Spring sẽ tự động lấy giá trị từ application.properties
    public CloudinaryServiceImpl(
            @Value("${cloudinary.cloud_name}") String cloudName,
            @Value("${cloudinary.api_key}") String apiKey,
            @Value("${cloudinary.api_secret}") String apiSecret) {
        
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }

    @Override
    public String uploadImage(MultipartFile file, String folderName) throws IOException {
        try {
            // Thiết lập tham số upload bao gồm folder để quản lý 4 bảng riêng biệt
            Map params = ObjectUtils.asMap(
                    "folder", "cinema_app/" + folderName,
                    "resource_type", "auto"
            );

            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), params);

            // Trả về secure_url (https) để đảm bảo bảo mật và hiển thị tốt khi deploy
            return uploadResult.get("secure_url").toString();
        } catch (IOException e) {
            throw new IOException("Lỗi trong quá trình tải ảnh lên Cloudinary: " + e.getMessage());
        }
    }

    @Override
    public void deleteImage(String imageUrl) throws IOException {
        try {
            // Cloudinary cần Public ID để xóa. ID thường nằm sau folder name và trước định dạng file.
            // Ví dụ: cinema_app/banners/abc.jpg -> Public ID là cinema_app/banners/abc
            String publicId = extractPublicId(imageUrl);
            
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (Exception e) {
            throw new IOException("Không thể xóa ảnh trên Cloudinary: " + e.getMessage());
        }
    }

    // Hàm bổ trợ để tách lấy Public ID từ URL Cloudinary
    private String extractPublicId(String url) {
        try {
            // Tìm vị trí bắt đầu của thư mục gốc "cinema_app"
            int startPos = url.indexOf("cinema_app");
            // Tìm vị trí dấu chấm cuối cùng (trước định dạng file .jpg, .png)
            int endPos = url.lastIndexOf(".");
            
            if (startPos != -1 && endPos != -1) {
                return url.substring(startPos, endPos);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}