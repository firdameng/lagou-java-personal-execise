import java.util.Random;
import java.util.concurrent.Semaphore;

/**
 * @Author CaiWencheng
 * @Date 2021-09-21 10:40
 */
public class MyThread extends Thread{
    private final Semaphore semaphore;
    private final Random random = new Random();

    public MyThread(Semaphore semaphore,String name){
        super(name);
        this.semaphore = semaphore;
    }

    @Override
    public void run() {
        try {
            semaphore.acquire();

            System.out.println(Thread.currentThread().getName()+"- 抢座成功，开始写作业");
            Thread.sleep(random.nextInt(1000));
            System.out.println(Thread.currentThread().getName()+"- 作业完成，离开座位");

        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            semaphore.release();
        }
    }
}
