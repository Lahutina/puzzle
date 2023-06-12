package com.lahutina.puzzle_mvc.service;

import com.lahutina.puzzle_mvc.entity.PieceInfo;
import org.springframework.core.io.Resource;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

public interface PuzzleService {

    boolean checkCorrectOrder(List<PieceInfo> pieceInfoList);

    boolean createPuzzle(MultipartFile imageFile, int numPuzzlePieces) throws IOException;

    boolean setImages(Model model);

    Resource readImage(String filename) throws MalformedURLException;
}
