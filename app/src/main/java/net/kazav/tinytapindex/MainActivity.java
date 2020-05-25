package net.kazav.tinytapindex;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.core.FirestoreClient;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private String TAG = "TinyTapIndex - Main Activity";
    private String username;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private int RC_SIGN_IN = 756;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Spinner snpAge = findViewById(R.id.spnAge);
        final Spinner snpLan = findViewById(R.id.spnLan);
        final Spinner snpCat = findViewById(R.id.spnCat);
        Button btnSend = findViewById(R.id.btnSend);

        SpinnerAdapterItems[] ageArray = new SpinnerAdapterItems[]{
                new SpinnerAdapterItems(2, "2-3"),
                new SpinnerAdapterItems(3, "3-4"),
                new SpinnerAdapterItems(4, "4-5"),
                new SpinnerAdapterItems(5, "5-6"),
                new SpinnerAdapterItems(6, "6-7"),
                new SpinnerAdapterItems(7, "7-8"),
                new SpinnerAdapterItems(8, "8-9"),
                new SpinnerAdapterItems(9, "9-10"),
                new SpinnerAdapterItems(10, "10-11"),
                new SpinnerAdapterItems(11, "11-12"),
                new SpinnerAdapterItems(12, "12-13"),
                new SpinnerAdapterItems(13, "13-14"),
                new SpinnerAdapterItems(14, "14+"),
                new SpinnerAdapterItems(0, "All Ages")
        };

        SpinnerAdapterItems[] lanArray = new SpinnerAdapterItems[]{
                new SpinnerAdapterItems(2, "Hebrew"),
                new SpinnerAdapterItems(0, "All"),
                new SpinnerAdapterItems(38, "Afrikaans"),
                new SpinnerAdapterItems(24, "Arabic"),
                new SpinnerAdapterItems(28, "Bulgarian"),
                new SpinnerAdapterItems(34, "Catalan"),
                new SpinnerAdapterItems(11, "Chinese (SM)"),
                new SpinnerAdapterItems(60, "Chinese (TR)"),
                new SpinnerAdapterItems(48, "Croatian"),
                new SpinnerAdapterItems(20, "Czech"),
                new SpinnerAdapterItems(13, "Danish"),
                new SpinnerAdapterItems(6, "Dutch"),
                new SpinnerAdapterItems(1, "English"),
                new SpinnerAdapterItems(53, "English (AU)"),
                new SpinnerAdapterItems(52, "English (UK)"),
                new SpinnerAdapterItems(39, "Estonian"),
                new SpinnerAdapterItems(32, "Euskara"),
                new SpinnerAdapterItems(33, "Filipino"),
                new SpinnerAdapterItems(26, "Finnish"),
                new SpinnerAdapterItems(4, "French"),
                new SpinnerAdapterItems(50, "Galego"),
                new SpinnerAdapterItems(5, "German"),
                new SpinnerAdapterItems(17, "Greek"),
                new SpinnerAdapterItems(37, "Hindi"),
                new SpinnerAdapterItems(16, "Hungarian"),
                new SpinnerAdapterItems(12, "Indonesian"),
                new SpinnerAdapterItems(7, "Italian"),
                new SpinnerAdapterItems(8, "Japanese"),
                new SpinnerAdapterItems(18, "Korean"),
                new SpinnerAdapterItems(56, "Latvian"),
                new SpinnerAdapterItems(45, "Lithuanian"),
                new SpinnerAdapterItems(31, "Malay"),
                new SpinnerAdapterItems(57, "Maldivian"),
                new SpinnerAdapterItems(27, "Norwegian"),
                new SpinnerAdapterItems(21, "Persian"),
                new SpinnerAdapterItems(25, "Polish"),
                new SpinnerAdapterItems(9, "Portuguese"),
                new SpinnerAdapterItems(19, "Romanian"),
                new SpinnerAdapterItems(10, "Russian"),
                new SpinnerAdapterItems(29, "Slovak"),
                new SpinnerAdapterItems(58, "Slovenian"),
                new SpinnerAdapterItems(3, "Spanish"),
                new SpinnerAdapterItems(30, "Swahili"),
                new SpinnerAdapterItems(22, "Swedish"),
                new SpinnerAdapterItems(49, "Tamil"),
                new SpinnerAdapterItems(46, "Telugu"),
                new SpinnerAdapterItems(15, "Thai"),
                new SpinnerAdapterItems(23, "Turkish"),
                new SpinnerAdapterItems(59, "Ukranian"),
                new SpinnerAdapterItems(14, "Vietnamese"),
                new SpinnerAdapterItems(55, "Welsh")
        };

        SpinnerAdapterItems[] catArray = new SpinnerAdapterItems[]{
                new SpinnerAdapterItems(508, "Early Childhood"),
                new SpinnerAdapterItems(0, "All Categories"),
                new SpinnerAdapterItems(104, "Language Studies"),
                new SpinnerAdapterItems(8, "Math"),
                new SpinnerAdapterItems(105, "English"),
                new SpinnerAdapterItems(34, "Nature"),
                new SpinnerAdapterItems(7, "Foreign Languages"),
                new SpinnerAdapterItems(132, "Music &amp; Arts"),
                new SpinnerAdapterItems(17, "Social Skills"),
                new SpinnerAdapterItems(433, "Therapy"),
                new SpinnerAdapterItems(45, "Holidays"),
                new SpinnerAdapterItems(4, "Social Studies"),
                new SpinnerAdapterItems(14, "Science"),
                new SpinnerAdapterItems(10, "Just for fun"),
                new SpinnerAdapterItems(12, "Tinypedia"),
                new SpinnerAdapterItems(451, "Videos"),
                new SpinnerAdapterItems(5, "Stories"),
                new SpinnerAdapterItems(74, "Speciality"),
                new SpinnerAdapterItems(13, "Assessment"),
                new SpinnerAdapterItems(1, "General"),
                new SpinnerAdapterItems(97, "Trivia"),
                new SpinnerAdapterItems(504, "Non-Verbal"),
        };

        ArrayAdapter ageAdp = new ArrayAdapter(this, android.R.layout.simple_spinner_item, ageArray);
        ArrayAdapter lanAdp = new ArrayAdapter(this, android.R.layout.simple_spinner_item, lanArray);
        ArrayAdapter catAdp = new ArrayAdapter(this, android.R.layout.simple_spinner_item, catArray);
        snpAge.setAdapter(ageAdp);
        snpLan.setAdapter(lanAdp);
        snpCat.setAdapter(catAdp);

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int language = ((SpinnerAdapterItems)(snpLan.getSelectedItem())).get_value();
                int age = ((SpinnerAdapterItems)(snpAge.getSelectedItem())).get_value();
                int category = ((SpinnerAdapterItems)(snpCat.getSelectedItem())).get_value();
                moveToContent(language, age, category);
            }
        });

        mAuth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void updateUI(FirebaseUser user) {
        if (user == null) {
            findViewById(R.id.btnSend).setVisibility(View.INVISIBLE);
            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
            SignInButton signInButton = findViewById(R.id.sign_in_button);
            signInButton.setSize(SignInButton.SIZE_WIDE);
            signInButton.setOnClickListener(new View.OnClickListener() {@Override public void onClick(View v) { signIn(); }});
        } else {
            findViewById(R.id.sign_in_button).setVisibility(View.INVISIBLE);
            findViewById(R.id.btnSend).setVisibility(View.VISIBLE);
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            final LocalDateTime now = LocalDateTime.now();
            db.collection("login").document(username).set(new HashMap<String, Long>(){{
                put(now.toString(), now.toEpochSecond(ZoneOffset.UTC));
            }}, SetOptions.merge());
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        username = currentUser.getEmail();
        updateUI(currentUser);
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.w(TAG, "Google sign in failed", e);
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            username = user.getEmail();
                            updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            updateUI(null);
                        }
                    }
                });
    }

    private void moveToContent(int language, int age, int category) {
        Intent content = new Intent(this, ContentActivity.class);
        content.putExtra("language", language);
        content.putExtra("age", age);
        content.putExtra("category", category);
        content.putExtra("username", username);
        startActivity(content);
        finish();
    }

    private class SpinnerAdapterItems {
        private int value;
        private String name;

        SpinnerAdapterItems(int val, String title) {
            value = val;
            name = title;
        }

        int get_value() {return value;}

        @NonNull
        @Override
        public String toString() {
            return name;
        }
    }
}
