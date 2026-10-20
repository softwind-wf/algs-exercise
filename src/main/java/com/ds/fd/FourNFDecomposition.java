package com.ds.fd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * 4NF decomposition algorithm from Figure 8-16.
 *
 * The input is a relation schema R, a set D of functional dependencies (FDs)
 * and multivalued dependencies (MVDs).  The output is a lossless decomposition
 * in which every relation is in 4NF.
 */
public class FourNFDecomposition {

    /** Functional dependency alpha -> beta. */
    static final class FD {
        final Set<String> alpha;
        final Set<String> beta;

        FD(Set<String> alpha, Set<String> beta) {
            this.alpha = Collections.unmodifiableSet(new HashSet<>(alpha));
            this.beta = Collections.unmodifiableSet(new HashSet<>(beta));
        }
    }

    /** Multivalued dependency alpha ->-> beta. */
    static final class MVD {
        final Set<String> alpha;
        final Set<String> beta;

        MVD(Set<String> alpha, Set<String> beta) {
            this.alpha = Collections.unmodifiableSet(new HashSet<>(alpha));
            this.beta = Collections.unmodifiableSet(new HashSet<>(beta));
        }

        String display() {
            return join(alpha) + " ->-> " + join(beta);
        }
    }

    static final class DependencySet {
        final Set<FD> fds;
        final Set<MVD> mvds;

        DependencySet(Set<FD> fds, Set<MVD> mvds) {
            this.fds = Collections.unmodifiableSet(new HashSet<>(fds));
            this.mvds = Collections.unmodifiableSet(new HashSet<>(mvds));
        }
    }

    private static Set<String> setOf(String... values) {
        return new HashSet<>(java.util.Arrays.asList(values));
    }

    private static String join(Set<String> attributes) {
        return String.join(",", new TreeSet<>(attributes));
    }

    private static Set<String> union(Set<String> left, Set<String> right) {
        Set<String> result = new HashSet<>(left);
        result.addAll(right);
        return result;
    }

    private static Set<String> difference(Set<String> left, Set<String> right) {
        Set<String> result = new HashSet<>(left);
        result.removeAll(right);
        return result;
    }

    private static Set<String> intersection(Set<String> left, Set<String> right) {
        Set<String> result = new HashSet<>(left);
        result.retainAll(right);
        return result;
    }

    private static boolean contains(Set<String> outer, Set<String> inner) {
        return outer.containsAll(inner);
    }

    /** FD closure of attributes under deps.fds. */
    private static Set<String> fdClosure(Set<String> attributes, DependencySet deps) {
        Set<String> result = new HashSet<>(attributes);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (FD fd : deps.fds) {
                if (contains(result, fd.alpha) && !contains(result, fd.beta)) {
                    result.addAll(fd.beta);
                    changed = true;
                }
            }
        }
        return result;
    }

    private static boolean isSuperkey(Set<String> attributes, Set<String> schema, DependencySet deps) {
        return contains(fdClosure(attributes, deps), schema);
    }

    /**
     * MVDs from D restricted to schema, closed under complementation.
     * FDs in D also derive MVDs on the schema.
     */
    private static Set<MVD> mvdsHoldingOn(Set<String> schema, DependencySet deps) {
        Set<MVD> result = new LinkedHashSet<>();
        for (MVD mvd : deps.mvds) {
            if (contains(schema, mvd.alpha)) {
                Set<String> beta = intersection(mvd.beta, schema);
                if (!beta.isEmpty()) {
                    result.add(new MVD(mvd.alpha, beta));
                }
            }
        }

        boolean changed = true;
        while (changed) {
            changed = false;
            for (MVD mvd : new ArrayList<>(result)) {
                Set<String> complement = difference(difference(schema, mvd.alpha), mvd.beta);
                if (!complement.isEmpty()) {
                    changed |= addMvd(result, new MVD(mvd.alpha, complement));
                }
            }
            for (FD fd : deps.fds) {
                if (contains(schema, fd.alpha)) {
                    Set<String> beta = intersection(fd.beta, schema);
                    if (!beta.isEmpty()) {
                        changed |= addMvd(result, new MVD(fd.alpha, beta));
                    }
                }
            }
        }
        return result;
    }

    private static boolean addMvd(Set<MVD> result, MVD candidate) {
        if (candidate.beta.isEmpty()) {
            return false;
        }
        for (MVD existing : result) {
            if (existing.alpha.equals(candidate.alpha) && existing.beta.equals(candidate.beta)) {
                return false;
            }
        }
        result.add(candidate);
        return true;
    }

    /**
     * Find a nontrivial MVD whose left side is not a superkey of schema,
     * or return null when schema is in 4NF.
     */
    private static MVD find4NFViolation(Set<String> schema, DependencySet deps) {
        Set<FD> restrictedFds = new HashSet<>();
        for (FD fd : deps.fds) {
            if (contains(schema, union(fd.alpha, fd.beta))) {
                restrictedFds.add(fd);
            }
        }
        DependencySet restricted = new DependencySet(restrictedFds, deps.mvds);

        List<MVD> holding = new ArrayList<>(mvdsHoldingOn(schema, restricted));
        Collections.sort(holding, new Comparator<MVD>() {
            @Override
            public int compare(MVD left, MVD right) {
                int bySize = Integer.compare(left.alpha.size(), right.alpha.size());
                if (bySize != 0) {
                    return bySize;
                }
                int byBetaSize = Integer.compare(left.beta.size(), right.beta.size());
                if (byBetaSize != 0) {
                    return byBetaSize;
                }
                return left.display().compareTo(right.display());
            }
        });

        for (MVD mvd : holding) {
            Set<String> rest = difference(difference(schema, mvd.alpha), mvd.beta);
            Set<String> meaningfulBeta = difference(mvd.beta, mvd.alpha);
            if (!meaningfulBeta.isEmpty() && !rest.isEmpty()
                    && !isSuperkey(mvd.alpha, schema, restricted)) {
                return mvd;
            }
        }
        return null;
    }

    /** Decompose schema into 4NF, returning the relations. */
    static List<Set<String>> decompose4NF(
            Set<String> schema, DependencySet deps, List<String> steps) {
        Set<Set<String>> result = new LinkedHashSet<>();
        result.add(new HashSet<>(schema));

        while (true) {
            Set<String> violatingSchema = null;
            MVD violation = null;
            for (Set<String> candidate : result) {
                violation = find4NFViolation(candidate, deps);
                if (violation != null) {
                    violatingSchema = candidate;
                    break;
                }
            }

            if (violation == null) {
                break;
            }

            result.remove(violatingSchema);
            result.add(difference(violatingSchema, violation.beta));
            result.add(union(violation.alpha, violation.beta));
            if (steps != null) {
                steps.add("Split " + join(violatingSchema)
                        + " using " + violation.display()
                        + " -> " + join(difference(violatingSchema, violation.beta))
                        + " + " + join(union(violation.alpha, violation.beta)));
            }
        }

        List<Set<String>> ordered = new ArrayList<>(result);
        Collections.sort(ordered, new Comparator<Set<String>>() {
            @Override
            public int compare(Set<String> left, Set<String> right) {
                return join(left).compareTo(join(right));
            }
        });
        return ordered;
    }

    private static void demo() {
        Set<String> schema = setOf("ID", "dept_name", "street", "city");
        Set<FD> fds = new HashSet<>();
        Set<MVD> mvds = new HashSet<>();
        mvds.add(new MVD(setOf("ID"), setOf("dept_name")));

        List<String> steps = new ArrayList<>();
        List<Set<String>> decomposition = decompose4NF(schema, new DependencySet(fds, mvds), steps);

        System.out.println("Original schema: " + join(schema));
        for (int i = 0; i < steps.size(); i++) {
            System.out.println("Step " + (i + 1) + ": " + steps.get(i));
        }
        System.out.println("4NF result:");
        for (Set<String> relation : decomposition) {
            System.out.println("  " + join(relation));
        }
    }

    public static void main(String[] args) {
        demo();
    }
}
