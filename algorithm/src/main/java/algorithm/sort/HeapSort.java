package algorithm.sort;

import java.util.List;

/**
 * Create by peiheng.jiang on 2019/9/3
 */
public class HeapSort implements Sort {

    @Override
    public int[] sort(int[] array) {

        return new int[0];
    }

    private void swap(int[] array, int index1, int index2) {
        int tmp = array[index1];
        array[index1] = array[index2];
        array[index2] = tmp;
    }

    public static void main(String[] args) {

    }

}
