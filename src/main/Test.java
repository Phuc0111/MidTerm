package main;

import java.io.*;
import java.security.MessageDigest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.xml.parsers.*;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.*;
import org.xml.sax.*;

public class Test {
    public static void main(String[] args) {
        // Tạo và khởi chạy các luồng
        Thread thread1 = new Thread(new FileReadingThread());
        Thread thread2 = new Thread(new AgeCalculationThread());
        Thread thread3 = new Thread(new PrimeCheckThread());
        thread1.start();
        
        try {
            thread1.join(); // Chờ thread1 kết thúc trước khi khởi chạy thread2 và thread3
            thread2.start();
            thread2.join(); // Chờ thread2 kết thúc trước khi khởi chạy thread3
            thread3.start();
            thread3.join(); // Chờ thread3 kết thúc trước khi tiếp tục
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Ghi kết quả vào file kq.xml
        writeResultToXML(FileReadingThread.students);
        
        for (Student student : FileReadingThread.students) {
            printStudentInfo(student);
        }
        
    }

    // Hàm ghi kết quả vào file kq.xml
    private static void writeResultToXML(List<Student> students) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            Element rootElement = doc.createElement("Students");
            doc.appendChild(rootElement);

            for (Student student : students) {
                Element studentElement = doc.createElement("Student");
                rootElement.appendChild(studentElement);

                if (student.getName() == null || student.getName().isEmpty()) {
                    studentElement.setAttribute("age", "empty");
                    studentElement.setAttribute("sum", "empty");
                    studentElement.setAttribute("isDigit", "empty");
                } else {
                    studentElement.setAttribute("age", String.valueOf(student.getAge()));
                    studentElement.setAttribute("sum", String.valueOf(student.getSum()));
                    studentElement.setAttribute("isDigit", String.valueOf(student.isPrime()));

                    // Thêm tuổi vào như là một phần tử con của studentElement
                    Element ageElement = doc.createElement("Age");
                    ageElement.appendChild(doc.createTextNode(String.valueOf(student.getAge())));
                    studentElement.appendChild(ageElement);
                }
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File("kq.xml"));
            transformer.transform(source, result);

            System.out.println("Kết quả đã được ghi vào file kq.xml");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void printStudentInfo(Student student) {
        System.out.println("Student ID: " + student.getId());
        System.out.println("Name: " + student.getName());
        System.out.println("Address: " + student.getAddress());
        System.out.println("Date of Birth: " + student.getDateOfBirth());
        System.out.println("Age: " + student.getAge());
        System.out.println("Sum: " + student.getSum());
        System.out.println("Is Digit Prime: " + student.isPrime());
        System.out.println("-----------------------------------");
    }
    
}

// Lớp đại diện cho học sinh
class Student {
    private String id;
    private String name;
    private String address;
    private String dateOfBirth;
    private int age;
    private int sum;
    private boolean isPrime;

    public Student(String id, String name, String address, String dateOfBirth) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.dateOfBirth = dateOfBirth;
    }

    // Getters and setters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getDateOfBirth() { return dateOfBirth; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public int getSum() { return sum; }
    public void setSum(int sum) { this.sum = sum; }
    public boolean isPrime() { return isPrime; }
    public void setPrime(boolean prime) { isPrime = prime; }
}

// Thread 1: Đọc file student.xml
class FileReadingThread implements Runnable {
    public static List<Student> students = new ArrayList<>();

    @Override
    public void run() {
        try {
            File file = new File("student.xml");
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);

            NodeList nodeList = doc.getElementsByTagName("Student");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Element element = (Element) nodeList.item(i);
                String id = element.getAttribute("id");
                String name = element.getElementsByTagName("Name").item(0).getTextContent();
                String address = element.getElementsByTagName("Address").item(0).getTextContent();
                String dateOfBirth = element.getElementsByTagName("DateOfBirth").item(0).getTextContent();
                students.add(new Student(id, name, address, dateOfBirth));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

//Thread 2: Tính tuổi và mã hóa chữ số của ngày sinh theo SHA-256
class AgeCalculationThread implements Runnable {
 @Override
 public void run() {
     for (Student student : FileReadingThread.students) {
         String dateOfBirth = student.getDateOfBirth();
         // Tính tuổi từ ngày sinh
         try {
             SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
             Date birthDate = sdf.parse(dateOfBirth);
             Calendar cal = Calendar.getInstance();
             int currentYear = cal.get(Calendar.YEAR);
             cal.setTime(birthDate);
             int birthYear = cal.get(Calendar.YEAR);
             int age = currentYear - birthYear;
             student.setAge(age);
         } catch (ParseException e) {
             e.printStackTrace();
         }

//          Mã hóa chữ số bằng SHA-256
         try {
             MessageDigest digest = MessageDigest.getInstance("SHA-256");
             byte[] encodedHash = digest.digest(dateOfBirth.getBytes());
             String hashedDateOfBirth = bytesToHex(encodedHash);
             // Lưu kết quả vào thuộc tính sum của student
             student.setSum(hashedDateOfBirth.hashCode());
         } catch (Exception e) {
             e.printStackTrace();
         }
     }
 }

 // Hàm chuyển đổi byte array sang dạng hex string
 private static String bytesToHex(byte[] hash) {
     StringBuilder hexString = new StringBuilder(2 * hash.length);
     for (byte b : hash) {
         String hex = Integer.toHexString(0xff & b);
         if (hex.length() == 1) {
             hexString.append('0');
         }
         hexString.append(hex);
     }
     return hexString.toString();
 }
}

// Thread 3: Kiểm tra số nguyên tố
class PrimeCheckThread implements Runnable {
    @Override
    public void run() {
        for (Student student : FileReadingThread.students) {
            // Kiểm tra số nguyên tố
            int sum = student.getSum();
            if (sum <= 1) {
                student.setPrime(false);
            } else {
                boolean isPrime = true;
                for (int i = 2; i <= Math.sqrt(sum); i++) {
                    if (sum % i == 0) {
                        isPrime = false;
                        break;
                    }
                }
                student.setPrime(isPrime);
            }
        }
    }
}