package com.benliebkemann;

import java.awt.FileDialog;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.benliebkemann.simulation.Simulation;
import com.benliebkemann.viewer.Camera;
import com.benliebkemann.viewer.Renderer;
import com.benliebkemann.viewer.SaveRenderer;
import com.benliebkemann.viewer.WindowRenderer;

public class App {

    public static void main(String[] args) {

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
        WindowRenderer windowRenderer = new WindowRenderer(renderSize, renderSize, c,
                saveRenderer);

        JFrame frame = new JFrame("FluidSim");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(windowRenderer.panel);
        frame.pack();
        frame.setSize(900, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        Thread t = new Thread() {
            public void run() {

                Simulation simulation = new Simulation();
                simulation.addRenderer(windowRenderer);
                simulation.run();
            }
        };
        t.start();
    }
}
