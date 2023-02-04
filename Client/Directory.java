package Client;

import java.io.File;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

public class Directory {
    private DefaultMutableTreeNode root; // File node
    private DefaultTreeModel treeModel;
    private JTree tree;

    public JTree getTree() {
        File[] diskRoots = java.io.File.listRoots();
        root = new DefaultMutableTreeNode();
        for (File fileSystemRoot : diskRoots) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(fileSystemRoot);
            File[] fileList = fileSystemRoot.listFiles();
            if (fileList != null) {
                for (File f : fileList) {
                    DefaultMutableTreeNode nodeChild = new DefaultMutableTreeNode(f);
                    node.add(nodeChild);
                    new Thread(new CreateChildNodes(f, nodeChild)).start();
                }
            }
            root.add(node);
        }

        treeModel = new DefaultTreeModel(root);
        tree = new JTree(treeModel);
        tree.setShowsRootHandles(true);
        return tree;
    }

    public class CreateChildNodes implements Runnable {
        private DefaultMutableTreeNode root;
        private File fileRoot;

        public CreateChildNodes(File fileRoot,
                DefaultMutableTreeNode root) {
            this.fileRoot = fileRoot;
            this.root = root;
        }

        @Override
        public void run() {
            createChildren(fileRoot, root);
        }

        private void createChildren(File fileRoot,
                DefaultMutableTreeNode node) {
            File[] files = fileRoot.listFiles();
            if (files == null)
                return;

            for (File file : files) {
                DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(file);
                node.add(childNode);
                if (file.isDirectory()) {
                    createChildren(file, childNode);
                }
            }
        }
    }
}