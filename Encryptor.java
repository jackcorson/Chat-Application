import java.util.*;

public class Encryptor {
    private String               message;
    private ArrayList<Character> list;
    private ArrayList<Character> shuffledPublicList;
    private ArrayList<Character> shuffledPrivateList;
    private char[]               charArray;

    public Encryptor() {
        list = new ArrayList<>();
        shuffledPublicList = new ArrayList<>();
        shuffledPrivateList = new ArrayList<>();
        generateKey();
    }

    public String encrypt(String message, String publicKey) {
        this.message = message;
        charArray = this.message.toCharArray();
        char[] pubToChar = publicKey.toCharArray();
        ArrayList<Character> clientPublicKey = new ArrayList<>();
        for (char letter : pubToChar) {
            clientPublicKey.add(letter);
        }
        
        this.message = message;
        for (int i = 0; i < charArray.length; i++) {
            for (int j = 0; j < list.size(); j++) {
                if (charArray[i] == list.get(j)) {
                    charArray[i] = clientPublicKey.get(j);
                    break;
                }
            }
        }
        return charArray.toString();
    }

    public String decrypt(String msg) {
        char[] chararr = msg.toCharArray();
        for (int i = 0; i < chararr.length; i++) {
            for (int j = 0; j < shuffledPrivateList.size(); j++) {
                if (chararr[i] == shuffledPrivateList.get(j)) {
                    chararr[i] = list.get(j);
                    break;
                }
            }
        }

        return chararr.toString();
    }

    private void generateKey() {
        for (int i = 0; i < 127; i++) {
            list.add((char) i);
        }

        shuffledPublicList = new ArrayList<>(list);
        Collections.shuffle(shuffledPublicList);
        shuffledPrivateList = new ArrayList<>(shuffledPublicList);
        Collections.shuffle(shuffledPrivateList);
    }

    public ArrayList<Character> getPublicKey() {
        return shuffledPublicList;
    }

    private ArrayList<Character> getPrivateKey() {
        return shuffledPrivateList;
    }

}