package vuatiengvietpj;

import vuatiengvietpj.controller.DictionaryController;

public class ClientApp {
    public static void main(String[] args) {
        try {
            DictionaryController dict = new DictionaryController("localhost", 2206);
            dict.lookupWord("apple");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
