package algorithm.sort;

/**
 * 排序接口
 * Create by peiheng.jiang on 2019/8/9
 */
public interface Sort {

    int[] sort(int[] array);

    default void print(int[] array) {
        for (int i : array) {
            System.out.print(i + " ");
        }
        System.out.println();
    }
}
