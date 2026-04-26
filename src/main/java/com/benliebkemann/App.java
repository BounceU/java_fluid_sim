package com.benliebkemann;

import java.awt.FileDialog;
import java.io.File;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.benliebkemann.simulation.Simulation;
import com.benliebkemann.viewer.Camera;
import com.benliebkemann.viewer.DensityCSVSaveRenderer;
import com.benliebkemann.viewer.DensityRenderer;
import com.benliebkemann.viewer.DensitySaveRenderer;
import com.benliebkemann.viewer.PressureSaveRenderer;
import com.benliebkemann.viewer.Renderer;
import com.benliebkemann.viewer.SaveRenderer;

public class App {

    public static void main(String[] args) throws IOException {

        SwingUtilities.invokeLater(App::createAndShowUi);

    }

    private static void createAndShowUi() {

        String filePath = null;
        while (filePath == null) {
            FileDialog fileDialog = new FileDialog((java.awt.Frame) null, "Select an output folder", FileDialog.LOAD);

            fileDialog.setVisible(true);

            String directory = fileDialog.getDirectory();

            if (directory != null) {
                File selectedFile = new File(directory);
                System.out.println("Got file " + selectedFile.getAbsolutePath());
                filePath = selectedFile.getAbsolutePath();
            } else {
                System.exit(0);
            }
        }

        Camera c = new Camera();
        int renderSize = 1000;
        c.scale = renderSize / 0.2;
        c.x = 0;
        c.y = 0;
        Renderer saveRenderer = new SaveRenderer(renderSize, renderSize, c,
                filePath, "outputImages",
                null);
        Renderer densitySaveRenderer = new DensitySaveRenderer(renderSize, renderSize, c,
                filePath, "densityImages",
                saveRenderer);
        Renderer pressureSaveRenderer = new PressureSaveRenderer(renderSize, renderSize, c,
                filePath, "pressureImages",
                densitySaveRenderer);
        Renderer densitySaveCSVRdnerer = new DensityCSVSaveRenderer(renderSize, renderSize, c, filePath,
                "densityValues", pressureSaveRenderer);
        // WindowRenderer windowRenderer = new WindowRenderer(renderSize, renderSize, c,
        // saveRenderer);

        DensityRenderer densityRenderer = new DensityRenderer(renderSize, renderSize, c, densitySaveCSVRdnerer);

        JFrame frame = new JFrame("FluidSim");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(densityRenderer.panel);
        frame.pack();
        frame.setSize(900, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        Thread t = new Thread() {
            public void run() {

                Simulation simulation = new Simulation();
                simulation.addRenderer(densityRenderer);
                simulation.run();
            }
        };
        t.start();
    }
}
