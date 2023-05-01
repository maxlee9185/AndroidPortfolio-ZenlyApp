package android.portfolio.zenlyapp

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.portfolio.zenlyapp.databinding.ActivityLoginBinding
import android.portfolio.zenlyapp.databinding.ActivityLoginEmailBinding
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class EmailLoginActivity :AppCompatActivity()  {

    private lateinit var binding : ActivityLoginEmailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginEmailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.doneButton.setOnClickListener{
            if(binding.emailEditText.text.isNotEmpty()){
                val data = Intent().apply{
                    putExtra("email",binding.emailEditText.text.toString() )
                }
                setResult(RESULT_OK, data)
                finish()
            }
            else{
                Toast.makeText(this,"이메일을 입력하세요",Toast.LENGTH_SHORT).show()
            }
        }
    }
}