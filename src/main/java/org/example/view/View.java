package org.example.view;

import javax.swing.*;
import java.awt.*;

public class View extends JFrame{

    private JTextArea rdfText;
    private JButton generateGraph;
    private JButton clear;
    private JButton generateRDF;
    private JButton fileChooserButton;
    private JLabel graphImage;
    private JLabel fileName;

    public View()
    {
        initializeUI();
    }

    private void initializeUI() {
        setTitle("RDF Graph Generator");
        setSize(1500, 1400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Natural Language Processing");
        title.setFont(new Font("Arial", Font.BOLD, 20));
        JPanel p1 = new JPanel();
        p1.add(Box.createVerticalStrut(50));
        p1.add(title);

        fileChooserButton = new JButton("Browse Files");
        generateRDF = new JButton("Generate RDF/XML");
        fileName = new JLabel("");
        JPanel p2 = new JPanel();
        p2.add(Box.createVerticalStrut(100));
        p2.add(fileChooserButton);
        p2.add(fileName);
        p2.add(generateRDF);
        mainPanel.add(Box.createVerticalStrut(50));

        mainPanel.add(p1);
        mainPanel.add(p2);

        rdfText = new JTextArea(40, 40);
        JScrollPane scrollPane = new JScrollPane(rdfText);
        mainPanel.add(scrollPane);

        generateGraph = new JButton("Generate Graph");
        clear = new JButton("Clear Text");

        JPanel middle = new JPanel(new FlowLayout());
        middle.add(generateGraph);
        middle.add(clear);
        mainPanel.add(middle);


        graphImage = new JLabel();
        graphImage.setPreferredSize(new java.awt.Dimension(1500,700));
        graphImage.setAlignmentX(CENTER_ALIGNMENT);
        JScrollPane scrollPane2 = new JScrollPane(graphImage);
        // Set the scroll bar policy to always show the bars
        scrollPane2.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane2.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane2.setViewportView(graphImage);
        mainPanel.add(scrollPane2);

        add(mainPanel);
        setVisible(true);

    }

    public JButton getClearButton()
    {return clear;}
    public JButton getGenerateGraphButton()
    {return generateGraph;}
    public JButton getGenerateRdfButton()
    {return generateRDF;}
    public JButton getFileChooserButton()
    {return fileChooserButton;}
    public JTextArea getRdfTextArea()
    {return rdfText;}
    public JLabel getFileNameLabel()
    {return fileName;}
    public JLabel getGraphImageLabel()
    {return graphImage;}
}
