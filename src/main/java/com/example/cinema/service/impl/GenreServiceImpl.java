package com.example.cinema.service.impl;

import com.example.cinema.dto.GenreRequest;
import com.example.cinema.entity.Genre;
import com.example.cinema.exception.ResourceNotFoundException;
import com.example.cinema.repository.GenreRepository;
import com.example.cinema.service.GenreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GenreServiceImpl implements GenreService {

    private final GenreRepository genreRepository;

    @Override
    public List<Genre> getAllGenres() {
        return genreRepository.findAll();
    }

    @Override
    public Genre getGenreById(Integer id) {
        return genreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Thể loại không tồn tại với ID: " + id));
    }

    @Transactional
    public Genre createGenre(GenreRequest request) {

        genreRepository.findByName(request.getName())
                .ifPresent(g -> {
                    throw new RuntimeException("Tên thể loại đã tồn tại!");
                });

        Genre genre = new Genre();
        genre.setName(request.getName());
        genre.setDescription(request.getDescription());

        return genreRepository.save(genre);
    }
    
    @Override
    @Transactional
    public Genre updateGenre(Integer id, GenreRequest request) {

        Genre genre = genreRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy thể loại để cập nhật")
                );

        if (!genre.getName().equalsIgnoreCase(request.getName())) {

            genreRepository.findByName(request.getName())
                    .ifPresent(existing -> {
                        throw new RuntimeException("Tên thể loại đã tồn tại!");
                    });
        }

        genre.setName(request.getName());
        genre.setDescription(request.getDescription());

        return genreRepository.save(genre);
    }

    @Override
    @Transactional
    public void deleteGenre(Integer id) {
        if (!genreRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy thể loại để xóa");
        }
        genreRepository.deleteById(id);
    }
}