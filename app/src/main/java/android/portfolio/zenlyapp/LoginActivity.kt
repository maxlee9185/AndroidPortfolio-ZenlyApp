package android.portfolio.zenlyapp

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.portfolio.zenlyapp.databinding.ActivityLoginBinding
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.kakao.sdk.user.model.User

class LoginActivity : AppCompatActivity() {

    private val callback: (OAuthToken?,Throwable? )->Unit = { token, error->
        if(error != null){
            //로그인실패
            showErrorToast()
        }else if(token !=null){
            //로그인 성공
            getKakaoAccountInfo()
            Log.e("loginActivity","login with kakao account token:$token")
        }

    }
    private lateinit var binding : ActivityLoginBinding
    private lateinit var emailLoginResult :ActivityResultLauncher< Intent>
    private lateinit var pendingUser : User
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        KakaoSdk.init(this,"345c2905e9b44ba545af50e860710029")

        emailLoginResult =registerForActivityResult( ActivityResultContracts.StartActivityForResult() ) {
            if(it.resultCode == RESULT_OK){
                val email = it.data?.getStringExtra("email")
                if(email==null){
                    showErrorToast()
                    return@registerForActivityResult
                }else{
                    signInFirebase(pendingUser,email )
                }
            }
        }

        binding.kakaoTalkLoginButton.setOnClickListener {
            if(UserApiClient.instance.isKakaoTalkLoginAvailable(this)){
                //카카오톡 로그인
                UserApiClient.instance.loginWithKakaoTalk(this){token, error ->
                    if(error!=null){
                        //카카오톡 로그인 실패
                        if(error is ClientError && error.reason==ClientErrorCause.Cancelled){
                            return@loginWithKakaoTalk
                        }
                        UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
                    }
                    else if(token !=null){
                        //Log.e("loginActivity","token==$token")
                        if(Firebase.auth.currentUser==null)
                        {
                            //카카오톡에서 정보 가져와 파이어베이스 로그인
                            getKakaoAccountInfo()
                        }
                        else
                        {
                            navigateToMapActivity()
                        }
                    }
                }
            }
            else{
                //카카오 계정 로그인
                UserApiClient.instance.loginWithKakaoAccount(this, callback= callback)
            }

        }
    }

    private fun showErrorToast(){
        Toast.makeText(this,"사용자 로그인에 실패했습니다.",Toast.LENGTH_SHORT).show()
    }

    private fun getKakaoAccountInfo() {
        UserApiClient.instance.me{ user, error ->
            if( error != null){
                showErrorToast()
                Log.e("LoginActivity","error cause: $error")
                error.printStackTrace()
            }else if (user !=null) {
                Log.e("LoginActivity","user : 회원번호:${user.id} / 이메일:${user.kakaoAccount?.email} / 닉네임:${user.kakaoAccount?.profile?.nickname} / 이미지:${user.kakaoAccount?.profile?.thumbnailImageUrl}")
                checkKakaoUserData(user)
            }
        }
    }

    private fun checkKakaoUserData(user: User) {
        val kakaoEmail = user.kakaoAccount?.email.orEmpty()

        if(kakaoEmail.isEmpty()){
            pendingUser = user
            emailLoginResult.launch( Intent(this,EmailLoginActivity::class.java))
            return
        }
        signInFirebase(user, kakaoEmail)
    }
    private fun signInFirebase(user:User, email:String)
    {
        val uid = user.id.toString()
        Firebase.auth.createUserWithEmailAndPassword(email,uid).addOnCompleteListener() {
            if(it.isSuccessful){
            //다음과정
                updateFirebaseDatabase(user)
            }
            else{ showErrorToast()}
        }.addOnFailureListener{
            if(it is FirebaseAuthUserCollisionException){
                Firebase.auth.signInWithEmailAndPassword(email,uid).addOnCompleteListener{ result->
                    if(result.isSuccessful){
                        //다음과정
                        updateFirebaseDatabase(user)
                    }
                    else{showErrorToast()}
                }.addOnFailureListener{ error->
                    showErrorToast()
                    error.printStackTrace()
                }
            }
            else{
                showErrorToast()
            }
        }
    }

    private fun updateFirebaseDatabase(user:User){
        val uid= Firebase.auth.currentUser?.uid.orEmpty()
        val personMap = mutableMapOf<String,Any>()
        personMap["uid"]=uid
        personMap["name"]=user.kakaoAccount?.profile?.nickname.orEmpty()
        personMap["profilePhoto"]=user.kakaoAccount?.profile?.thumbnailImageUrl.orEmpty()
        Firebase.database.reference.child("Person").child(uid).updateChildren(personMap)

        navigateToMapActivity()
    }
    private fun navigateToMapActivity() {
        startActivity(Intent(this,MapActivity::class.java))
    }
}