package com.crummenauerca.cripto;

import javax.crypto.*;
import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;

public class Alice {
    public static void main(String[] args) {
        try {
            System.out.println("[Alice] Selecionando um arquivo...");
            JFileChooser jFileChooser = new JFileChooser("");
            if (jFileChooser.showDialog(new JFrame(), "OK") == JFileChooser.APPROVE_OPTION) {
                System.out.println("[Alice] Extraindo conteúdo do arquivo...");
                File file = jFileChooser.getSelectedFile();
                FileInputStream fileInputStream = new FileInputStream(file);
                byte[] content = new byte[(int) fileInputStream.getChannel().size()];
                fileInputStream.read(content);

                System.out.println("[Alice] Gerando par de chaves RSA para garantir integridade...");
                KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
                keyPairGenerator.initialize(1024);
                KeyPair keyPair = keyPairGenerator.generateKeyPair();

                System.out.println("[Alice] Conectando pela porta 5555...");
                Socket socket = new Socket("localhost", 5555);
                System.out.println("[Alice] Conexão realizada. Oiee, Bob S2");

                System.out.println("[Alice] Gerando chave de sessão AES");
                SecretKey secretKey = KeyGenerator.getInstance("AES").generateKey();

                System.out.println("[Alice] Criptografando conteúdo usando a chave de sessão AES...");
                Cipher cipherAES = Cipher.getInstance("AES");
                cipherAES.init(Cipher.ENCRYPT_MODE, secretKey);
                byte[] encryptedContent = cipherAES.doFinal(content);

                System.out.println("[Alice] Recebendo pacote de dados com a chave RSA pública de Bob...");
                ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
                Object object = (Object) objectInputStream.readObject();
                System.out.println("[Alice] Pacote recebido");

                System.out.println("[Alice] Criptografando chave de sessão AES usando a chave privada RSA de Bob...");
                Cipher cipherRSA = Cipher.getInstance("RSA");
                cipherRSA.init(Cipher.ENCRYPT_MODE, object.getPublicKeyBob());
                byte[] encryptedSessionKey = cipherRSA.doFinal(secretKey.getEncoded());

                System.out.println("[Alice] Empacotando chave RSA pública de Alice para garantir integridade...");
                object.setPublicKeyAlice(keyPair.getPublic());
                System.out.println("[Alice] Empacotando chave de sessão AES (criptografada)...");
                object.setEncryptedSessionKey(encryptedSessionKey);
                System.out.println("[Alice] Empacotando conteúdo criptografado de arquivo...");
                object.setEncryptedContent(encryptedContent);
                System.out.println("[Alice] Empacotando nome do arquivo...");
                object.setFileName(file.getName());

                System.out.println("[Alice] Serializando pacote de dados...");
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
                objectOutputStream.writeObject(object);
                objectOutputStream.close();
                byteArrayOutputStream.close();

                System.out.println("[Alice] Gerando hash do pacote serializado...");
                byte[] hash = MessageDigest.getInstance("SHA-256").digest(byteArrayOutputStream.toByteArray());

                System.out.println("[Alice] Criptografando hash...");
                cipherRSA.init(Cipher.ENCRYPT_MODE, keyPair.getPrivate());
                byte[] signature = cipherRSA.doFinal(hash);

                System.out.println("[Alice] Adicionando assinatura digital no pacote...");
                object.setSignature(signature);

                System.out.println("[Alice] Enviando pacote de dados...");
                objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                objectOutputStream.writeObject(object);
                System.out.println("[Alice] Dados enviados");

                System.out.println("[Alice] Encerrando a conexão. Adeus Bob :(");
                socket.close();
            } else {
                System.out.println("[Alice] A seleção de arquivos foi ignorada :(");
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}