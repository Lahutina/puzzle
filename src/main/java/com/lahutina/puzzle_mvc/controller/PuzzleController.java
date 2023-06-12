package com.lahutina.puzzle_mvc.controller;

import com.lahutina.puzzle_mvc.entity.PieceInfo;
import com.lahutina.puzzle_mvc.service.PuzzleService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class PuzzleController {

    private final PuzzleService puzzleService;

    @RequestMapping("/")
    public String homePage() {
        return "index";
    }

    @PostMapping("/upload")
    public String createPuzzle(@RequestParam("imageFile") MultipartFile imageFile, @RequestParam("numPuzzlePieces") int numPuzzlePieces) throws IOException {
        return puzzleService.createPuzzle(imageFile, numPuzzlePieces) ? "redirect:/puzzles" : "redirect:/";
    }

    @GetMapping("/puzzles")
    public String displayPuzzle(Model model) {
        return puzzleService.setImages(model) ? "puzzles" : "redirect:/";
    }

    @GetMapping("/image/{filename}")
    @ResponseBody
    public ResponseEntity<Resource> readImage(@PathVariable("filename") String filename) throws MalformedURLException {
        return ResponseEntity.ok().body(puzzleService.readImage(filename));
    }

    @PostMapping("/submit")
    @ResponseBody
    public boolean submitImageInfo(@RequestBody List<PieceInfo> pieceInfoList) {
        return puzzleService.checkCorrectOrder(pieceInfoList);
    }
}
