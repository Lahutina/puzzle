package com.lahutina.puzzle_mvc.service.impl;

import com.lahutina.puzzle_mvc.entity.PieceInfo;
import com.lahutina.puzzle_mvc.entity.PuzzleInfo;
import com.lahutina.puzzle_mvc.entity.Sides;
import com.lahutina.puzzle_mvc.service.PuzzleService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.List;

@Service
public class PuzzleServiceImpl implements PuzzleService {

    private static final String UPLOAD_DIRECTORY = System.getProperty("user.dir") + "/myPuzzle";
    private static final String PUZZLE_DIRECTORY = System.getProperty("user.dir") + "/myPuzzle/pieces";

    @Override
    public boolean createPuzzle(MultipartFile imageFile, int numPuzzlePieces) {
        if (imageFile.isEmpty()) return false;

        try {
            // create directory to save image
            File puzzlePiecesPath = new File(UPLOAD_DIRECTORY);
            if (!puzzlePiecesPath.exists()) {
                puzzlePiecesPath.mkdirs();
            }

            // save image
            String filename = Objects.requireNonNull(imageFile.getOriginalFilename());
            Path filePath = Path.of(UPLOAD_DIRECTORY, filename);
            Files.copy(imageFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // create puzzle pieces from this image
            divideImageToPieces(filePath.toFile(), numPuzzlePieces);

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void divideImageToPieces(File imagePath, int numPuzzlePieces) throws IOException {
        // create new repository for pieces
        File puzzlePiecesPath = new File(PUZZLE_DIRECTORY);
        if (!puzzlePiecesPath.exists()) {
            puzzlePiecesPath.mkdirs();
        }

        // split image to pieces
        BufferedImage image = ImageIO.read(imagePath);
        BufferedImage[] puzzlePieces = createPuzzles(image, numPuzzlePieces);

        // save pieces to directory with unique names
        for (BufferedImage piece : puzzlePieces) {
            String puzzlePieceFileName = "puzzle_piece_" + Math.random() * 100 + ".jpg";
            File puzzlePieceFile = new File(puzzlePiecesPath.getAbsolutePath() + "/" + puzzlePieceFileName);
            ImageIO.write(piece, "jpg", puzzlePieceFile);
        }

        // set data about saved puzzle, width, height main image and puzzle pieces num
        PuzzleInfo.setData(image.getWidth(), image.getHeight(), puzzlePieces.length, image);
    }

    private BufferedImage[] createPuzzles(BufferedImage sourceImage, int numPuzzlePieces) {
        // num of columns and rows in our puzzle
        int numCols = (int) Math.sqrt(numPuzzlePieces);
        int numRows = numPuzzlePieces / numCols;

        // width and height of one puzzle piece
        int pieceWidth = (int) Math.round((double) sourceImage.getWidth() / numCols);
        int pieceHeight = (int) Math.round((double) sourceImage.getHeight() / numRows);

        // split image into pieces and save them in array
        BufferedImage[] puzzlePieces = new BufferedImage[numPuzzlePieces];
        int index = 0;

        for (int y = 0; y < numRows; y++) {
            for (int x = 0; x < numCols; x++) {
                BufferedImage piece = new BufferedImage(pieceWidth, pieceHeight, sourceImage.getType());
                Graphics2D g = piece.createGraphics();
                g.drawImage(sourceImage, 0, 0, pieceWidth, pieceHeight, x * pieceWidth, y * pieceHeight, (x + 1) * pieceWidth, (y + 1) * pieceHeight, null);
                puzzlePieces[index] = piece;
                g.dispose();
                index++;
            }
        }

        return puzzlePieces;
    }

    @Override
    public boolean setImages(Model model) {
        // read pieces from directory
        List<String> puzzleNames = new ArrayList<>();
        File puzzlesFolder = new File(PUZZLE_DIRECTORY);

        if (puzzlesFolder.isDirectory()) {
            File[] puzzleFiles = puzzlesFolder.listFiles();
            if (puzzleFiles != null) {
                for (File puzzleFile : puzzleFiles) {
                    puzzleNames.add(puzzleFile.getName());
                }
            }
        }

        // set attributes to html file
        model.addAttribute("width", PuzzleInfo.getWidth());
        model.addAttribute("height", PuzzleInfo.getHeight());
        model.addAttribute("puzzles", puzzleNames);

        return true;
    }

    @Override
    public Resource readImage(String filename) throws MalformedURLException {
        String imagePath = PUZZLE_DIRECTORY + "/" + filename;
        return new UrlResource("file:" + imagePath);
    }

    @Override
    public boolean checkCorrectOrder(List<PieceInfo> pieceInfoList) {
        // determine the dimensions of the grid
        int numCols = (int) Math.sqrt(pieceInfoList.size());
        int numRows = pieceInfoList.size() / numCols;

        // create two-dimensional array for later to merge pieces
        PieceInfo[][] puzzleGrid = new PieceInfo[numRows][numCols];

        int row, col;
        for (row = 0; row < numRows; row++) {
            // get the top left piece
            PieceInfo topLeftPiece = findTopLeft(pieceInfoList);

            // check all pieces in one row
            for (col = 0; col < numCols - 1; col++) {
                // check with neighbour piece
                PieceInfo closestPiece = findClosestHorizontally(pieceInfoList, topLeftPiece);
                if (comparePieces(topLeftPiece, closestPiece, Sides.RIGHT, Sides.LEFT)) return false;

                // check with top piece if in row > 0
                if (row > 0)
                    if (comparePieces(topLeftPiece, puzzleGrid[row - 1][col], Sides.TOP, Sides.BOTTOM)) return false;

                puzzleGrid[row][col] = topLeftPiece;
                pieceInfoList.remove(topLeftPiece);
                topLeftPiece = closestPiece;
            }

            // for last piece in a row
            if (row > 0)
                if (comparePieces(topLeftPiece, puzzleGrid[row - 1][col], Sides.TOP, Sides.BOTTOM)) return false;
            puzzleGrid[row][col] = topLeftPiece;
            pieceInfoList.remove(topLeftPiece);
        }

        // Create final image from pieces and save it
        BufferedImage finalImage = createFinalImage(puzzleGrid, numCols, numRows);
        String outputFileName = "final_image.jpg";

        try {
            File outputFile = new File(UPLOAD_DIRECTORY, outputFileName);
            ImageIO.write(finalImage, "jpg", outputFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    private BufferedImage createFinalImage(PieceInfo[][] puzzleGrid, int numCols, int numRows) {
        // width and height of initial image
        int puzzleWidth = PuzzleInfo.getWidth();
        int puzzleHeight = PuzzleInfo.getHeight();

        // width and height of one piece
        int puzzlePieceWidth = puzzleWidth / numCols;
        int puzzlePieceHeight = puzzleHeight / numRows;

        //  reate a new BufferedImage to hold the final image
        BufferedImage finalImage = new BufferedImage(puzzleWidth, puzzleHeight, PuzzleInfo.getImage().getType());
        Graphics2D g2d = finalImage.createGraphics();

        // iterate over the puzzleGrid array
        for (int row = 0; row < numRows; row++) {
            for (int col = 0; col < numCols; col++) {
                PieceInfo pieceInfo = puzzleGrid[row][col];
                BufferedImage puzzlePieceImage = loadImage(pieceInfo);

                // calculate the position to draw the puzzle piece in the final image
                int x = col * puzzlePieceWidth;
                int y = row * puzzlePieceHeight;

                // draw the puzzle piece onto the final image
                g2d.drawImage(puzzlePieceImage, x, y, null);
            }
        }

        g2d.dispose();

        return finalImage;
    }


    private PieceInfo findTopLeft(List<PieceInfo> pieceInfoList) {
        // find by x left - min, find all that are +- 15 pixels in X like min, find top by Y
        PieceInfo minX = pieceInfoList.stream().min(Comparator.comparingInt(PieceInfo::getX)).orElse(null);
        return pieceInfoList.stream().filter(p -> Math.abs(p.getX() - minX.getX()) < 15).min(Comparator.comparingInt(PieceInfo::getY)).orElse(null);
    }

    private PieceInfo findClosestHorizontally(List<PieceInfo> pieceInfoList, PieceInfo topLeftPiece) {
        // find by y - min, find all that are +- pixels in Y like min, find top by X
        PieceInfo minY = pieceInfoList.stream().filter(p -> p != topLeftPiece).min(Comparator.comparingInt(piece -> Math.abs(piece.getY() - topLeftPiece.getY()))).orElse(null);
        return pieceInfoList.stream().filter(p -> Math.abs(p.getY() - minY.getY()) < 15 && p != topLeftPiece).min(Comparator.comparingInt(PieceInfo::getX)).orElse(null);
    }

    private boolean comparePieces(PieceInfo puzzle1, PieceInfo puzzle2, Sides image1Side, Sides image2Side) {
        // load images from directory
        BufferedImage image1 = loadImage(puzzle1);
        BufferedImage image2 = loadImage(puzzle2);

        // rotate them if needed
        if (puzzle1.getRotation() != 0) {
            image1 = rotateImage(image1, puzzle1.getRotation());
        }
        if (puzzle2.getRotation() != 0) {
            image2 = rotateImage(image2, puzzle2.getRotation());
        }

        // get arrays of pixels on sides to compare
        int[] firstArrayPixels = getSidePixels(image1, image1Side);
        int[] secondArrayPixels = getSidePixels(image2, image2Side);

        // check color similarity between the pixels
        double colorDifferenceThreshold = 55.0;
        return !areColumnsSimilar(firstArrayPixels, secondArrayPixels, colorDifferenceThreshold);
    }

    private BufferedImage loadImage(PieceInfo pieceInfo) {
        try {
            String imagePath = pieceInfo.getPath().substring(pieceInfo.getPath().lastIndexOf("/") + 1);
            return ImageIO.read(new File(PUZZLE_DIRECTORY + "/" + imagePath));
        } catch (IOException e) {
            throw new RuntimeException("Error loading puzzle piece image: " + pieceInfo.getPath(), e);
        }
    }

    private int[] getSidePixels(BufferedImage image, Sides side) {
        int width = image.getWidth();
        int height = image.getHeight();

        int[] pixels;

        switch (side) {
            case LEFT, RIGHT -> pixels = new int[height];
            case TOP, BOTTOM -> pixels = new int[width];
            default -> pixels = new int[0];
        }

        getColumnPixels(image, side, pixels);

        return pixels;
    }

    private BufferedImage rotateImage(BufferedImage image, int rotation) {
        int width = image.getWidth();
        int height = image.getHeight();

        // Create a new BufferedImage with the same dimensions
        BufferedImage rotatedImage = new BufferedImage(width, height, image.getType());

        // Get the graphics context of the rotated image
        Graphics2D g2d = rotatedImage.createGraphics();

        // Perform the rotation based on the specified angle
        switch (rotation) {
            case 90:
                g2d.rotate(Math.toRadians(90));
                g2d.drawImage(image, 0, -width, height, width, null);
                break;
            case 180:
                g2d.rotate(Math.toRadians(180));
                g2d.drawImage(image, -width, -height, width, height, null);
                break;
            case 270:
                g2d.rotate(Math.toRadians(270));
                g2d.drawImage(image, -height, 0, height, width, null);
                break;
            default:
                return image;
        }

        g2d.dispose();

        return rotatedImage;
    }

    private void getColumnPixels(BufferedImage image, Sides side, int[] arrayOfPixels) {
        switch (side) {
            case LEFT -> {
                int leftColumnX = 0; // Leftmost column
                int leftColumnY = 0; // Topmost row
                image.getRGB(leftColumnX, leftColumnY, 1, image.getHeight(), arrayOfPixels, 0, 1);
            }
            case RIGHT -> {
                int rightColumnX = image.getWidth() - 1; // Rightmost column
                int rightColumnY = 0; // Topmost row
                image.getRGB(rightColumnX, rightColumnY, 1, image.getHeight(), arrayOfPixels, 0, 1);
            }
            case BOTTOM -> {
                int bottomRowX = 0; // Leftmost column
                int bottomRowY = image.getHeight() - 1; // Bottom row
                image.getRGB(bottomRowX, bottomRowY, image.getWidth(), 1, arrayOfPixels, 0, image.getWidth());
            }
            case TOP -> {
                int topRowX = 0; // Leftmost column
                int topRowY = 0; // Topmost row
                image.getRGB(topRowX, topRowY, image.getWidth(), 1, arrayOfPixels, 0, image.getWidth());
            }
        }
    }


    private static boolean areColumnsSimilar(int[] column1Pixels, int[] column2Pixels, double threshold) {
        // Check if the lengths of the columns match
        if (column1Pixels.length != column2Pixels.length) {
            return false;
        }
        double average = 0;

        // Compare color similarity of each pair of pixels
        for (int i = 0; i < column1Pixels.length; i++) {
            int rgb1 = column1Pixels[i];
            int rgb2 = column2Pixels[i];

            // Calculate color difference between the two pixels
            double colorDifference = calculateColorDifference(rgb1, rgb2);

            average += colorDifference;
        }

        return average / column1Pixels.length < threshold;
    }

    private static double calculateColorDifference(int rgb1, int rgb2) {
        int r1 = (rgb1 >> 16) & 0xFF;
        int g1 = (rgb1 >> 8) & 0xFF;
        int b1 = rgb1 & 0xFF;

        int r2 = (rgb2 >> 16) & 0xFF;
        int g2 = (rgb2 >> 8) & 0xFF;
        int b2 = rgb2 & 0xFF;

        int deltaR = r1 - r2;
        int deltaG = g1 - g2;
        int deltaB = b1 - b2;

        // Calculate the sum of squared differences
        int sumSquaredDiff = deltaR * deltaR + deltaG * deltaG + deltaB * deltaB;

        // Calculate the square root of the sum (Euclidean distance)
        return Math.sqrt(sumSquaredDiff);
    }
}
