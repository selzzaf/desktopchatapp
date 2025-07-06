package com.chatrealtime.service;

import com.chatrealtime.model.User;
import com.google.firebase.database.*;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class ContactService {
    
    private final DatabaseReference contactsRef;
    private final DatabaseReference usersRef;
    
    public ContactService() {
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        this.contactsRef = db.getReference("contacts");
        this.usersRef = db.getReference("users");
    }
    
    public CompletableFuture<Void> addContact(String userId, String contactId) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        // Vérifier si l'utilisateur existe
        usersRef.child(contactId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    future.completeExceptionally(new IllegalArgumentException("Contact not found"));
                    return;
                }
                
                // Ajouter la relation bidirectionnelle
                contactsRef.child(userId).child(contactId).setValue(true, (error, ref) -> {
                    if (error != null) {
                        future.completeExceptionally(error.toException());
                    } else {
                        contactsRef.child(contactId).child(userId).setValue(true, (error2, ref2) -> {
                            if (error2 != null) {
                                future.completeExceptionally(error2.toException());
                            } else {
                                future.complete(null);
                            }
                        });
                    }
                });
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                future.completeExceptionally(error.toException());
            }
        });
        
        return future;
    }
    
    public CompletableFuture<List<User>> getContacts(String userId) {
        CompletableFuture<List<User>> future = new CompletableFuture<>();
        
        contactsRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<User> contacts = new ArrayList<>();
                List<CompletableFuture<Void>> futures = new ArrayList<>();
                
                for (DataSnapshot contactSnapshot : snapshot.getChildren()) {
                    String contactId = contactSnapshot.getKey();
                    CompletableFuture<Void> userFuture = new CompletableFuture<>();
                    futures.add(userFuture);
                    
                    usersRef.child(contactId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot userSnapshot) {
                            User contact = userSnapshot.getValue(User.class);
                            if (contact != null) {
                                contacts.add(contact);
                            }
                            userFuture.complete(null);
                        }
                        
                        @Override
                        public void onCancelled(DatabaseError error) {
                            userFuture.completeExceptionally(error.toException());
                        }
                    });
                }
                
                // Attendre que tous les contacts soient chargés
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenRun(() -> future.complete(contacts))
                    .exceptionally(e -> {
                        future.completeExceptionally(e);
                        return null;
                    });
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                future.completeExceptionally(error.toException());
            }
        });
        
        return future;
    }
    
    public CompletableFuture<Void> removeContact(String userId, String contactId) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        contactsRef.child(userId).child(contactId).removeValue((error, ref) -> {
            if (error != null) {
                future.completeExceptionally(error.toException());
            } else {
                contactsRef.child(contactId).child(userId).removeValue((error2, ref2) -> {
                    if (error2 != null) {
                        future.completeExceptionally(error2.toException());
                    } else {
                        future.complete(null);
                    }
                });
            }
        });
        
        return future;
    }
    
    public void subscribeToContactStatus(String userId, ContactStatusListener listener) {
        contactsRef.child(userId).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                String contactId = snapshot.getKey();
                subscribeToUserStatus(contactId, listener);
            }
            
            @Override
            public void onChildRemoved(DataSnapshot snapshot) {
                String contactId = snapshot.getKey();
                listener.onContactRemoved(contactId);
            }
            
            @Override
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) {}
            
            @Override
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) {}
            
            @Override
            public void onCancelled(DatabaseError error) {
                listener.onError(error.toException());
            }
        });
    }
    
    private void subscribeToUserStatus(String userId, ContactStatusListener listener) {
        usersRef.child(userId).child("status").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String status = snapshot.getValue(String.class);
                if (status != null) {
                    listener.onStatusChanged(userId, status);
                }
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                listener.onError(error.toException());
            }
        });
    }
    
    public interface ContactStatusListener {
        void onStatusChanged(String userId, String status);
        void onContactRemoved(String userId);
        void onError(Exception e);
    }
} 