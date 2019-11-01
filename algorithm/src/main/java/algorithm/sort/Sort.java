package algorithm.sort;

/**
 * 排序接口
 * Create by peiheng.jiang on 2019/8/9
 */
public interface Sort {

    int[] sort(int[] array);

    default boolean isValid(int[] array) {
        return array != null && array.length != 0;
    }

    default void print(int[] array) {
        for (int i : array) {
            System.out.print(i + " ");
        }
        System.out.println();
    }

    default void swap(int[] array, int i1, int i2) {
        int tmp = array[i1];
        array[i1] = array[i2];
        array[i2] = tmp;
    }
}
