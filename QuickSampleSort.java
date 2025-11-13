//Implementação do Quick Sample Sort em Java, incluindo os casos melhor e médio.
//Aluna: Caroline Freitas Alvernaz, matrícula: 773069
import java.util.*;
import java.util.concurrent.*;

public class QuickSampleSort {

    private static final int NUM_THREADS = 4;
    private static final int SAMPLE_FACTOR = 4;
    private static final int MAX_ELEMENTS = 100;

    public static void main(String[] args) throws InterruptedException {
        int[] array = generateDistinctRandomArray(MAX_ELEMENTS);

        System.out.println("Melhor caso:");
        int[] melhorCaso = Arrays.copyOf(array, array.length);
        quickSampleSortMelhorCaso(melhorCaso);

        System.out.println("\nCaso médio:");
        int[] casoMedio = Arrays.copyOf(array, array.length);
        quickSampleSortCasoMedio(casoMedio);

        Arrays.sort(array);
        System.out.println("\nVetor final ordenado:");
        System.out.println(Arrays.toString(array));
    }

    public static void quickSampleSortMelhorCaso(int[] A) {
        int n = A.length;
        if (n % NUM_THREADS != 0) throw new IllegalArgumentException("n deve ser múltiplo de " + NUM_THREADS);

        Arrays.sort(A);

        int q = n / NUM_THREADS;
        int[] pivots = new int[NUM_THREADS - 1];
        pivots[0] = A[q - 1];          // posição 24 quando n=100
        pivots[1] = A[2 * q - 1];      // posição 49 quando n=100
        pivots[2] = A[3 * q - 1];      // posição 74 quando n=100

        int[] shuffled = Arrays.copyOf(A, A.length);
        Random rand = new Random();
        for (int i = shuffled.length - 1; i > 0; i--) {
            int j = rand.nextInt(i + 1);
            int tmp = shuffled[i];
            shuffled[i] = shuffled[j];
            shuffled[j] = tmp;
        }
        System.arraycopy(shuffled, 0, A, 0, A.length);

        List<List<Integer>> buckets = new ArrayList<>();
        for (int i = 0; i < NUM_THREADS; i++) {
            buckets.add(Collections.synchronizedList(new ArrayList<>()));
        }

        for (int val : A) {
            int idx = 0;
            while (idx < pivots.length && val > pivots[idx]) idx++;
            buckets.get(idx).add(val);
        }

        for (int i = 0; i < buckets.size(); i++) {
            System.out.println("Subvetor " + (i + 1) + " (tamanho " + buckets.get(i).size() + "): " + buckets.get(i));
        }
    }

    public static void quickSampleSortCasoMedio(int[] A) throws InterruptedException {
        int n = A.length;
        int partSize = n / NUM_THREADS;
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);

        List<int[]> localSamples = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch1 = new CountDownLatch(NUM_THREADS);

        for (int t = 0; t < NUM_THREADS; t++) {
            final int start = t * partSize;
            final int end = (t == NUM_THREADS - 1) ? n : start + partSize;
            executor.submit(() -> {
                int[] local = Arrays.copyOfRange(A, start, end);
                Arrays.sort(local);
                int step = Math.max(1, local.length / SAMPLE_FACTOR);
                int[] sample = new int[SAMPLE_FACTOR];
                for (int i = 0; i < SAMPLE_FACTOR; i++) {
                    sample[i] = local[Math.min(i * step, local.length - 1)];
                }
                localSamples.add(sample);
                latch1.countDown();
            });
        }

        latch1.await();

        List<Integer> combinedSamples = new ArrayList<>();
        for (int[] s : localSamples) for (int v : s) combinedSamples.add(v);
        Collections.sort(combinedSamples);
        int[] pivots = new int[NUM_THREADS - 1];
        for (int i = 1; i < NUM_THREADS; i++) {
            pivots[i - 1] = combinedSamples.get(i * combinedSamples.size() / NUM_THREADS);
        }

        List<List<Integer>> buckets = new ArrayList<>();
        for (int i = 0; i < NUM_THREADS; i++) {
            buckets.add(Collections.synchronizedList(new ArrayList<>()));
        }

        CountDownLatch latch2 = new CountDownLatch(NUM_THREADS);
        for (int t = 0; t < NUM_THREADS; t++) {
            final int start = t * partSize;
            final int end = (t == NUM_THREADS - 1) ? n : start + partSize;
            executor.submit(() -> {
                for (int i = start; i < end; i++) {
                    int val = A[i];
                    int idx = 0;
                    while (idx < pivots.length && val > pivots[idx]) idx++;
                    buckets.get(idx).add(val);
                }
                latch2.countDown();
            });
        }

        latch2.await();
        executor.shutdown();

        for (int i = 0; i < buckets.size(); i++) {
            System.out.println("Subvetor " + (i + 1) + " (tamanho " + buckets.get(i).size() + "): " + buckets.get(i));
        }
    }

    private static int[] generateDistinctRandomArray(int n) {
        List<Integer> list = new ArrayList<>();
        for (int i = 1; i <= n * 2; i++) list.add(i);
        Collections.shuffle(list);
        return list.stream().limit(n).mapToInt(Integer::intValue).toArray();
    }
}
