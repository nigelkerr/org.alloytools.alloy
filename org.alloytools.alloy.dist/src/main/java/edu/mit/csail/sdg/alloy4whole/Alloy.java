package edu.mit.csail.sdg.alloy4whole;

import org.alloytools.alloy.lsp.provider.AlloyLanguageServer;

public class Alloy {

    public static void main(String args[]) throws Exception {
        if (args.length > 0 && args[0].equals("ls"))
            AlloyLanguageServer.main(args);
        else
            SimpleGUI.main(args);
    }
}
