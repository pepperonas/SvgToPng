/*
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package io.celox;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * The type Main.
 */
public class Main extends Application {

    private String pathTmpBashFile = System.getProperty("user.home") + "/.svg-to-png.sh";
    private TextField mTfDest;
    private List<String> svgFiles;


    @Override
    public void start(Stage primaryStage) throws Exception {
        BorderPane borderPane = new BorderPane();
        primaryStage.setTitle("svg-to-png");
        Scene scene = new Scene(borderPane, 500, 130);
        primaryStage.setScene(scene);
        primaryStage.show();

        VBox vBoxTop = setUpVboxTop();

        VBox vBoxCenter = setupVboxCenter();

        Button btnGoBottom = setUpButtonBottom();

        setUpPadding(vBoxTop, vBoxCenter);

        setUpBorderPane(borderPane, vBoxTop, vBoxCenter, btnGoBottom);
    }

    private VBox setUpVboxTop() {
        VBox vBoxTop = new VBox(4);
        Label label = new Label("DEST:");
        mTfDest = new TextField();
        mTfDest.setPromptText("drag dest");

        mTfDest.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.ANY);
            }
            event.consume();
        });
        mTfDest.setOnDragDropped(event -> {
            List<File> files = event.getDragboard().getFiles();
            for (File f : files) {
                if (f.isDirectory()) {
                    mTfDest.setText(f.getPath());
                    return;
                }
            }
            event.consume();
        });
        vBoxTop.getChildren().addAll(label, mTfDest);
        return vBoxTop;
    }

    private VBox setupVboxCenter() {
        Label label;
        VBox vBoxCenter = new VBox(4);
        label = new Label("SOURCE:");
        TextField tfSource = new TextField();
        tfSource.setPromptText("drag source");
        tfSource.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.ANY);
            }
            event.consume();
        });
        tfSource.setOnDragDropped(event -> {
            List<File> files = event.getDragboard().getFiles();
            System.out.println("Found " + files.size() + " files");
            new Thread(() -> Platform.runLater(() -> {
                if (files.get(0).isDirectory()) {
                    tfSource.setText(files.get(0).getPath());
                    checkForSvg(event);
                }
            })).start();
            event.consume();
        });

        vBoxCenter.getChildren().addAll(label, tfSource);
        return vBoxCenter;
    }

    private Button setUpButtonBottom() {
        Button btnGoBottom = new Button("GO!");
        btnGoBottom.setOnAction(event -> showDialog(svgFiles));
        return btnGoBottom;
    }

    private void setUpPadding(VBox vBoxTop, VBox vBoxCenter) {
        vBoxTop.setPadding(new Insets(2, 2, 2, 2));
        vBoxCenter.setPadding(new Insets(2, 2, 2, 2));
    }

    private void setUpBorderPane(BorderPane borderPane, VBox vBoxTop, VBox vBoxCenter,
        Button btnGoBottom) {
        borderPane.setTop(vBoxTop);
        borderPane.setCenter(vBoxCenter);
        borderPane.setBottom(btnGoBottom);
    }


    private void checkForSvg(DragEvent event) {
        svgFiles = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(Paths.get(event.getDragboard().getFiles().get
            (0).getAbsolutePath()))) {
            paths.forEach(filePath -> {
                if (Files.isRegularFile(filePath)) {
                    System.out.println(filePath);
                    if (filePath.toAbsolutePath().toString().endsWith(".svg")) {
                        svgFiles.add(filePath.toString());
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void showDialog(List<String> svgFiles) {
        Stage dialog = new Stage();
        dialog.setTitle("Set color");
        TextField tfColor = new TextField("#424242");
        TextField tfPngSize = new TextField("512");
        Button btnOk = new Button("OK");
        btnOk.setOnAction(event1 -> {
            for (String svg : svgFiles) {
                try {
                    String content = new String(Files.readAllBytes(Paths.get(svg)));
                    content = content.replaceAll("#(?:[0-9a-fA-F]{3}){1,2}", tfColor.getText());
                    alterSvgFile(svg, content);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            createBash(svgFiles, Integer.parseInt(tfPngSize.getText()));
            executeCommand("sh " + pathTmpBashFile);
            File bash = new File(pathTmpBashFile);
            bash.delete();
            svgFiles.clear();
        });
        VBox VBox = new VBox(tfColor, tfPngSize, btnOk);
        dialog.setScene(new Scene(VBox));
        dialog.show();
    }


    private void alterSvgFile(String svg, String content) {
        try {
            PrintWriter writer = new PrintWriter(svg, "UTF-8");
            writer.println(content);
            writer.close();
        } catch (IOException e) {
            System.out.println("Error while writing SVG..");
        }
    }


    private void createBash(List<String> svgFiles, int pngSize) {
        try {
            PrintWriter writer = new PrintWriter(pathTmpBashFile, "UTF-8");
            writer.println("#!/bin/sh");
            for (String item : svgFiles) {
                writer.println("rsvg-convert -h " + pngSize + " " + item + " > " + item.replace
                    (".svg", "" + ".png"));
                writer
                    .println("mv " + item.replace(".svg", ".png") + " " + mTfDest.getText() + "/");
            }
            writer.close();
        } catch (IOException e) {
            System.out.println("Error while creating bash..");
        }
    }


    private String executeCommand(String command) {
        StringBuilder output = new StringBuilder();
        Process p;
        try {
            p = Runtime.getRuntime().exec(command);
            p.waitFor();
            BufferedReader reader =
                new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return output.toString();
    }


    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}
