/*package template;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ASTAR {

    public static Node<State> optimalTraversal(Tree<State> graph){

        List<Node<State>> goalStates = new ArrayList<>();

        List<Node<State>> Q = new ArrayList<>(graph.root);
        Set<State> C = new HashSet<>();

        while(!Q.isEmpty()){
            Node<State> n = Q.remove(0);
            if(n.isFinal()){
                return n;

            }else if(!C.contains(n) || ){
                C.add(n);
                List<Node<State>> S = n.getChildren();
                Q.addAll(S);
            }
        }

        return null;
    }

}
*/