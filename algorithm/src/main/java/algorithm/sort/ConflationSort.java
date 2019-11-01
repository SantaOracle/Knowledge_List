package algorithm.sort;

/**
 * @author peiheng.jiang create on 2019/9/10
 */
public class ConflationSort implements Sort {

    @Override
    public int[] sort(int[] array) {
        if (!isValid(array)) {
            return array;
        }
        // temp array generate only once, better for GC, time cost and memery allocation
        conflationSort(array, 0, array.length - 1, new int[array.length]);
        return new int[0];
    }

    private void conflationSort(int[] array, int start, int end, int[] tmp) {
        if (start != end) {
            // split mission
            int mid = (start + end) / 2;
            conflationSort(array, start, mid, tmp);
            conflationSort(array, mid + 1, end, tmp);
            merge(array, start, end, tmp);
        }
    }

    private void merge(int[] array, int start, int end, int[] tmp) {
        int tmpIndex = 0;
        int tmpMaxIndex = end - start;
        int leftIndex = start;
        int leftMaxIndex = (start + end) / 2;
        int rightIndex = leftMaxIndex + 1;
        int rightMaxIndex = end;

        while (tmpIndex <= tmpMaxIndex) {
            if (leftIndex > leftMaxIndex) {
                for (int i = rightIndex; i <= rightMaxIndex; i++) {
                    tmp[tmpIndex] = array[i];
                    tmpIndex++;
                }
                break;
            }

            if (rightIndex > rightMaxIndex) {
                for (int i = leftIndex; i <= leftMaxIndex; i++) {
                    tmp[tmpIndex] = array[i];
                    tmpIndex++;
                }
                break;
            }

            if (array[leftIndex] < array[rightIndex]) {
                tmp[tmpIndex] = array[leftIndex];
                leftIndex++;
            } else {
                tmp[tmpIndex] = array[rightIndex];
                rightIndex++;
            }
            tmpIndex++;
        }

        tmpIndex = 0;
        for (int i = start; i <= end; i++) {
            array[i] = tmp[tmpIndex];
            tmpIndex++;
        }
    }

    public static void main(String[] args) {
        int[] arr = new int[]{3, 9, 4, 10, 25, 7, 0, 1};
        Sort sort = new ConflationSort();
        sort.sort(arr);
        sort.print(arr);
    }
}
