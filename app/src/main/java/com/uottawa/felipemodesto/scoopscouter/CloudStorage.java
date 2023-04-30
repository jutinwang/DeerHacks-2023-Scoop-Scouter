package com.uottawa.felipemodesto.scoopscouter;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class CloudStorage extends AppCompatActivity {
    public void AddData(Timestamp t, GeoPoint gp) {
        // Access a Cloud Firestore instance from your Activity
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DocumentReference docRef = db.collection("ice_cream_trucks").document("max_truck_id");
        docRef.get().addOnCompleteListener)(new OnCompleteListener<DocumentSnapshot>(
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    
        }
        ))
        Map<String, Object> truckEntry = new HashMap<>();


        // TODO: get id # from max_truck_id and assign it to this name and increment it


        truckEntry.put("location", gp);
        truckEntry.put("timestamp", t);

        DocumentReference newTruckRef = db.collection("ice_cream_trucks").document();

        newTruckRef.set(truckEntry);
    }

    public void GetNearbyTrucks() {
        // TODO: make up a formula
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference trucksRef = db.collection("ice_cream_trucks");
        Query nearbyQuery = trucksRef.whereEqualTo("")
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
