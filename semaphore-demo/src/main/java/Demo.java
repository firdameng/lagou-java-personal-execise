import java.util.concurrent.Semaphore;

/**
 * @Author CaiWencheng
 * @Date 2021-09-21 10:47
 */
public class Demo {
    public static void main(String[] args) {
        Semaphore semaphore = new Semaphore(2);
        for (int i = 0; i < 5; i++) {
            new MyThread(semaphore,"学生-"+i).start();
        }
    }
}
