package ua.safetynet;

        import android.support.annotation.NonNull;
        import android.util.Log;

        import com.google.android.gms.tasks.OnCompleteListener;
        import com.google.android.gms.tasks.OnSuccessListener;
        import com.google.android.gms.tasks.Task;
        import com.google.firebase.auth.FirebaseAuth;
        import com.google.firebase.firestore.CollectionReference;
        import com.google.firebase.firestore.DocumentReference;
        import com.google.firebase.firestore.DocumentSnapshot;
        import com.google.firebase.firestore.FirebaseFirestore;
        import com.google.firebase.firestore.Query;
        import com.google.firebase.firestore.QueryDocumentSnapshot;
        import com.google.firebase.firestore.QuerySnapshot;

        import java.util.ArrayList;
        import java.util.Map;

        import ua.safetynet.group.Group;
        import ua.safetynet.user.User;

public class Database {
    public interface DatabaseUserListener {
        void onUserRetrieval(User user);

    }
    public interface DatabaseGroupListener {
        void onGroupRetrieval(Group group);
    }
    public interface DatabaseGroupsListener {
        void onGroupsRetrieval(ArrayList<Group> groups);
    }
    public interface DatabaseTransactionsListener {
        void onTransactionsRetrieval(ArrayList<Transaction> transactions);
    }

    /** template
    public interface DatabaseUsersListener {
        void onUsersRetrieval(ArrayList<User> users);
    } */

    private DocumentReference databaseUser;
    private DocumentReference databaseGroup;
    private CollectionReference databaseUsers;
    private CollectionReference databaseGroups;
    private CollectionReference databaseTransactions;

    public Database() {
        this.databaseUsers = FirebaseFirestore.getInstance().collection("Users");
        this.databaseGroups = FirebaseFirestore.getInstance().collection("Groups");
        this.databaseTransactions = FirebaseFirestore.getInstance().collection("Transactions");
    }

    //returns the user ID of the user currently logged in to the device (via firebaseAuth)
    public String getUID(){
        return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    /**query the firestore and return an arraylist of the groups the current user is in
    Firestore queries are incapable of performing logical OR operations, so searching from the user's group list proved impossible*/
    public void queryGroups(final Database.DatabaseGroupsListener dbListener){
        Query groupQuery = databaseGroups
                .whereArrayContains("users", FirebaseAuth.getInstance().getCurrentUser().getUid());

        groupQuery.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()) {
                    ArrayList<Group> groupList = new ArrayList<>();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Group group = Group.fromMap(document.getData());
                        Log.d("DATABASE", document.toString());
                        //Group group = document.toObject(Group.class);
                        //add group to an arraylist
                        groupList.add(group);
                    }
                    dbListener.onGroupsRetrieval(groupList);
                }
                else{
                    //error toast message goes here
                    Log.d("DATABASE", "Group list fetch was not successful");
                }
            }
        });
    }

    /**
     * returns all the transactions for the currently logged in user in a specific group
     * The payments activity currently outputs userid instead of userId, so all references in to and from map are done as such
     * @param dbListener
     * @param groupId
     */
    public void queryUserTransactions(final Database.DatabaseTransactionsListener dbListener, String groupId){
        final ArrayList<Transaction> transactionList = new ArrayList<>();
        Query transactionQuery = databaseTransactions
                .whereEqualTo("userid", FirebaseAuth.getInstance().getCurrentUser().getUid())
                .whereEqualTo("groupId", groupId);

        transactionQuery.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()) {

                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Transaction transaction = new Transaction();
                        transaction = transaction.fromMap(document.getData());
                        //add Transaction to an arraylist
                        transactionList.add(transaction);
                    }
                    dbListener.onTransactionsRetrieval(transactionList);
                }
                else{
                    //error toast message goes here
                }
            }
        });
    }

    /**
     * returns all transactions for a specific group, regardless of the user involved.
     * @param Id
     * @param dbListener
     */
    public void queryGroupTransactions(String Id, final Database.DatabaseTransactionsListener dbListener){
        final ArrayList<Transaction> transactionList = new ArrayList<>();
        Query transactionQuery = databaseTransactions
                .whereArrayContains("groupId", Id);

        transactionQuery.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()) {

                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Transaction transaction = document.toObject(Transaction.class);
                        //add Transaction to an arraylist
                        transactionList.add(transaction);
                    }
                    dbListener.onTransactionsRetrieval(transactionList);
                }
                else{
                    //error toast message goes here
                }
            }
        });
    }

    /***
     * Takes in userId and database listener. Database listener passes in the User object from firestore
     * @param userId User ID from firebase auth. User ID is the key for the user data in Firestore
     * @param dbListener Listener is called when Firebase returns with the User object. Passes the user object as a parameter
     */
    public void getUser(String userId, final Database.DatabaseUserListener dbListener) {
        databaseUsers.document(userId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                User user = User.fromMap(documentSnapshot.getData());
                dbListener.onUserRetrieval(user);
            }
        });
    }

    public void getGroup(String groupID, final Database.DatabaseGroupListener dbListener) {
        databaseGroups.document(groupID).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                Group group = new Group();
                group = Group.fromMap(documentSnapshot.getData());
                dbListener.onGroupRetrieval(group);
            }
        });
    }

    //updates a user's data in firestore using a given user class
    public void setUser(User user) {
        databaseUsers.document(user.getUserId()).set(user);
    }

    //updates a group's data in firestore using a given group class
    public void setGroup(Group group){
        this.databaseGroups.document(group.getGroupId()).set(group);
    }

    //creates a new user entry in firestore. userId is set here.  Other values must be set before calling createGroup.
    public void createUser(User user){
        this.databaseUser = FirebaseFirestore.getInstance().collection("Users").document();
        user.setUserId(databaseUser.getId());
        databaseGroup.set(user);
    }

    //creates a new group entry in firestore. group_ID is set here.  Other values must be set before calling createGroup.
    public void createGroup(Group group){
        this.databaseGroup = FirebaseFirestore.getInstance().collection("Groups").document();
        group.setGroupId(databaseGroup.getId());
        Map<String, Object> map = group.toMap();
        databaseGroup.set(map);
    }

}

