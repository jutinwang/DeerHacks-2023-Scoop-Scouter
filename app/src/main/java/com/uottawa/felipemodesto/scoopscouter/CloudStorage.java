package com.uottawa.felipemodesto.scoopscouter;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class CloudStorage extends AppCompatActivity {
    public void AddData(Timestamp t, GeoPoint gp) {
        // Access a Cloud Firestore instance from your Activity
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DocumentReference maxIdRef = db.collection("indices").document("max_truck_id");
        final String TAG = "DocSnippets";
        maxIdRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    int id = 1;
                    if (document.exists()) {
                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                        // get id # from max_truck_id and assign it to this name and increment it
                        id = Integer.parseInt(document.get("id").toString());
                        Log.d(TAG, "Max ID: " + document.get("id").toString());
                        Log.d(TAG, "New ID: " + id);
                    } else {
                        Log.d(TAG, "No such document");
                    }
                    Map<String, Object> truckEntry = new HashMap<>();
                    String fileName = "truck_" + Integer.toString(id);
                    DocumentReference newTruckRef = db.collection("ice_cream_trucks").document(fileName);
                    truckEntry.put("location", gp);
                    truckEntry.put("timestamp", t);
                    newTruckRef.set(truckEntry);
                    maxIdRef.update("id", FieldValue.increment(1));
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });
    }

    public void GetNearbyTrucks() {
        // TODO: make up a formula
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference trucksRef = db.collection("ice_cream_trucks");
        //Query nearbyQuery = trucksRef.whereEqualTo("")
//        trucksRef
//                .whereEqualTo("capital", true)
//                .get()
//                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
//                    @Override
//                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
//                        if (task.isSuccessful()) {
//                            for (QueryDocumentSnapshot document : task.getResult()) {
//                                Log.d(TAG, document.getId() + " => " + document.getData());
//                            }
//                        } else {
//                            Log.d(TAG, "Error getting documents: ", task.getException());
//                        }
//                    }
//                });
    }

    public void GetImage(int id, int imgNum) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        // Create a storage reference from this app
        StorageReference storageRef = storage.getReference();
        // Creates a child reference to "scoop_scouter"
        StorageReference imagesRef = storageRef.child("ice-cream-truck-images/");
        String imgName = "truck" + id;

        StorageReference truckImgRef = imagesRef.child(imgName);
        final long ONE_MEGABYTE = 1024 * 1024;
        truckImgRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                // Data for "images/island.jpg" is returns, use this as needed
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure (@NonNull Exception exception) {
                // Handle any errors
            }
        });

    }

    public void GetImage(int id) {
        GetImage(id, 0);
    }
}
