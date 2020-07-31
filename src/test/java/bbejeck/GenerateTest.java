package bbejeck;

import bbejeck.clients.producer.MockDataProducer;


public class GenerateTest {

    public static void main(String[] args) {
        MockDataProducer.producePurchaseData();

        try {
            Thread.sleep(12*1000l);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        MockDataProducer.shutdown();
    }
}
