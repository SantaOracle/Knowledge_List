package algorithm.sort;

/**
 * @author peiheng.jiang create on 2019/9/10
 */
public class SelectionSort implements Sort{

    @Override
    public int[] sort(int[] array) {
        if (!isValid(array)) {
            return array;
        }
        selectionSort(array);
        return array;
    }

    private void selectionSort(int[] array) {
        for (int i = 0; i < array.length - 1; i++) {
            for (int j = i + 1; j < array.length; j++) {
                if (array[i] > array[j]) {
                    swap(array, i, j);
                }
            }
        }
    }

    public static void main(String[] args) {
        int[] arr = new int[]{3, 9, 4, 10, 25, 7, 0, 1};
        Sort sort = new SelectionSort();
        sort.sort(arr);
        sort.print(arr);
    }
}
