package me.stumper66.spawnercontrol;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class CachedModalList<T extends Comparable<T>> implements Cloneable {
    public CachedModalList(){
        this.allowedList = new TreeSet<>();
        this.excludedList = new TreeSet<>();
    }

    public CachedModalList(@NotNull final Set<T> allowedList, @NotNull final Set<T> excludedList){
        this.allowedList = allowedList;
        this.excludedList = excludedList;
    }

    @NotNull
    public Set<T> allowedList;
    @NotNull
    public Set<T> excludedList;
    public boolean doMerge;
    public boolean allowAll;
    public boolean excludeAll;

    public boolean isEnabledInList(final T item) {
        if (this.allowAll) return true;
        if (this.excludeAll) return false;
        //if (this.isEmpty()) return true;
        if (this.excludedList.contains(item)) return false;

        return this.allowedList.contains(item);
    }

    public boolean isEmpty(){
        return this.allowedList.isEmpty() &&
                this.excludedList.isEmpty();
    }

    public String toString(){
        final StringBuilder sb = new StringBuilder();
        if (!this.allowedList.isEmpty()){
            if (sb.length() > 0) sb.append(", ");
            sb.append("lst: ");
            sb.append(this.allowedList);
        }
        if (this.allowAll) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("all allowed");
        }

        if (this.excludeAll) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("all excluded");
        }

        if (!this.excludedList.isEmpty()){
            if (sb.length() > 0) sb.append(", ");
            sb.append("ex-lst: ");
            sb.append(this.excludedList);
        }

        return sb.toString();
    }
}