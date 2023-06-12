package com.lahutina.puzzle_mvc.entity;

import lombok.Getter;

import java.awt.image.BufferedImage;

@Getter
public class PuzzleInfo {
    private static int width;
    private static int height;
    private static int numPuzzlePieces;

    private static BufferedImage image;

    private PuzzleInfo() {
    }

    public static void setData(int width, int height, int numPuzzlePieces, BufferedImage image) {
        PuzzleInfo.width = width;
        PuzzleInfo.height = height;
        PuzzleInfo.numPuzzlePieces = numPuzzlePieces;
        PuzzleInfo.image = image;
    }

    public static int getWidth() {
        return width;
    }

    public static int getHeight() {
        return height;
    }

    public static int getNumPuzzlePieces() {
        return numPuzzlePieces;
    }

    public static BufferedImage getImage() {
        return image;
    }
}
