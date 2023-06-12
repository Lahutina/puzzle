package com.lahutina.puzzle_mvc.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PieceInfo {
    private int x;
    private int y;
    private int rotation;
    private String path;
}