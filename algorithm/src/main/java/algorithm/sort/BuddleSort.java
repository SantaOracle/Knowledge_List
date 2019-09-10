package algorithm.sort;

/**
 * @author peiheng.jiang create on 2019/9/10
 */
public class BuddleSort implements Sort {

    @Override
    public int[] sort(int[] array) {
        if (!isValid(array)) {
            return array;
        }
        buddleSort(array);
        return new int[0];
    }

    private void buddleSort(int[] array) {
        boolean hasSwap = false;
        for (int i = array.length - 1; i >= 0; i--) {
            for (int j = 0; j < i; j++) {
                if (array[j] > array[j + 1]) {
                    swap(array, j, j + 1);
                    hasSwap = true;
                }
            }
            if (!hasSwap) {
                break;
            }
        }
    }

    public static void main(String[] args) {
        int[] arr = new int[]{3, 9, 4, 10, 25, 7, 0, 1};
        Sort sort = new BuddleSort();
        sort.sort(arr);
        sort.print(arr);
    }
}
