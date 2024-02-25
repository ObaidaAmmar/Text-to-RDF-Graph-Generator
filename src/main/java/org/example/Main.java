package org.example;

import org.example.controller.Controller;
import org.example.model.Model;
import org.example.view.View;

public class Main {
    public static void main(String[] args)
    {
        Model model = new Model();
        View view = new View();
        Controller controller = new Controller(model, view);

    }
}