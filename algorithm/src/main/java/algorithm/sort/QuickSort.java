package algorithm.sort;

/**
 * 快排
 * 哨兵位选择待优化，目前是随机哨兵位
 * Create by peiheng.jiang on 2019/8/9
 */
public class QuickSort implements Sort{

    @Override
    public int[] sort(int[] array) {
        if (array == null || array.length < 2) {
            return array;
        }
        return quickSort(array, 0, array.length - 1);
    }

    private int[] quickSort(int[] array, int start, int end) {
        if (start >= end) {
            return array;
        }
        int midIndex = partition(array, start, end);
        if (midIndex > start) {
            array = quickSort(array, start, midIndex - 1);
        }
        if (midIndex < end) {
            array = quickSort(array, midIndex + 1, end);
        }
        return array;
    }

    private int partition(int[] array, int start, int end) {
        int privotIndex = start + (int) (Math.random() * (end - start + 1));
        swap(array, privotIndex, end);     //  把哨兵元素放到最后一位
        int smallEdge = start - 1;
        for (int index = start; index < end; index++) {
            if (array[index] < array[end]) {
                smallEdge++;
                if (smallEdge != index) {
                    swap(array, smallEdge, index);
                }
            }
        }
        swap(array, smallEdge + 1, end);
        return smallEdge + 1;
    }

    public static void main(String[] args) {
        int[] array = new int[]{4, 1, 7, 11, 9, 1, 2, 5, 2};
        Sort sort = new QuickSort();
        array = sort.sort(array);
        sort.print(array);
    }
}
