package com.example.cinema.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface CloudinaryService {
    /**
     * Tải ảnh lên Cloudinary
     * @param file: Tệp tin ảnh từ request
     * @param folderName: Tên thư mục trên Cloud (movies, banners, actors, users)
     * @return URL tuyệt đối của ảnh (https://...)
     */
    String uploadImage(MultipartFile file, String folderName) throws IOException;

    /**
     * Xóa ảnh trên Cloudinary khi không sử dụng
     * @param imageUrl: URL đầy đủ của ảnh cần xóa
     */
    void deleteImage(String imageUrl) throws IOException;
}