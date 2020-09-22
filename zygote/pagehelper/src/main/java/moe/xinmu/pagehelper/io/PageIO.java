package moe.xinmu.pagehelper.io;

import lombok.Data;

@Data
public class PageIO<T> {
    Class<T> targetClass;
    Integer page;
    Integer size;
    String order;

    public PageIO(Class<T> targetClass, int page, int size) {
        this.targetClass = targetClass;
        this.page = page;
        this.size = size;
    }

    public PageIO(Class<T> targetClass, int page, int size, String order) {
        this.targetClass = targetClass;
        this.page = page;
        this.size = size;
        this.order = order;
    }
}
