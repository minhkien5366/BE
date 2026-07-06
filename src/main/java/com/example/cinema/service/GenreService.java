package com.example.cinema.service;

import com.example.cinema.dto.GenreRequest;
import com.example.cinema.entity.Genre;
import java.util.List;

public interface GenreService {
    List<Genre> getAllGenres();
    Genre getGenreById(Integer id);
    Genre createGenre(GenreRequest request);
    Genre updateGenre(Integer id, GenreRequest request);
    void deleteGenre(Integer id);
}