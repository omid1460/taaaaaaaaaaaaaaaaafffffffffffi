<?php
$code_file = file_get_contents("cokh.php"); 
$code_file1 = explode('","', $code_file);
//header('refresh: 5');
//------------------------------------
$ra = rand (0,2048);
$ra1 = rand (0,2048);
$ra2 = rand (0,2048);
$ra3 = rand (0,2048);
$ra4 = rand (0,2048);
$ra5 = rand (0,2048);
$ra6 = rand (0,2048);
$ra7 = rand (0,2048);
$ra8 = rand (0,2048);
$ra9 = rand (0,2048);
$ra10 = rand (0,2048);
$ra11 = rand (0,2048);

//---------------------------
$ok = $code_file1[$ra];
$ok1 = $code_file1[$ra1];
$ok2 = $code_file1[$ra2];
$ok3 = $code_file1[$ra3];
$ok4 = $code_file1[$ra4];
$ok5 = $code_file1[$ra5];
$ok6 = $code_file1[$ra6];
$ok7 = $code_file1[$ra7];
$ok8 = $code_file1[$ra8];
$ok9 = $code_file1[$ra9];
$ok10 = $code_file1[$ra10];
$ok11 = $code_file1[$ra11];
//echo "$ok $ok1 $ok2 $ok3 $ok4 $ok5 $ok6 $ok7 $ok8 $ok9 $ok10 $ok11";
?>
<blockquote id="myInput"><?php echo "$ok $ok1 $ok2 $ok3 $ok4 $ok5 $ok6 $ok7 $ok8 $ok9 $ok10 $ok11";?>
</blockquote>

<button class="k2-copy-button" id="k2button"><svg aria-hidden="true" height="1em" preserveaspectratio="xMidYMid meet" role="img" viewbox="0 0 24 24" width="1em" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns="http://www.w3.org/2000/svg"><g fill="none"><path d="M13 6.75V2H8.75A2.25 2.25 0 0 0 6.5 4.25v13a2.25 2.25 0 0 0 2.25 2.25h9A2.25 2.25 0 0 0 20 17.25V9h-4.75A2.25 2.25 0 0 1 13 6.75z" fill="currentColor"><path d="M14.5 6.75V2.5l5 5h-4.25a.75.75 0 0 1-.75-.75z" fill="currentColor"><path d="M5.503 4.627A2.251 2.251 0 0 0 4 6.75v10.504a4.75 4.75 0 0 0 4.75 4.75h6.494c.98 0 1.813-.626 2.122-1.5H8.75a3.25 3.25 0 0 1-3.25-3.25l.003-12.627z" fill="currentColor"></path></path></path></g></svg>Copy</button>  


<style>
.k2-copy-button svg{margin-right: 10px;vertical-align: top;}  
.k2-copy-button{
  height: 45px; width: 155px; color: #fff; background: #265df2; outline: none; border: none; border-radius: 8px; font-size: 17px; font-weight: 400; margin: 8px 0; cursor: pointer; transition: all 0.4s ease;}
.k2-copy-button:hover{background: #2ECC71;}
@media (max-width: 480px) {#k2button{width: 100%;}}
</style>
<script>
  function copyFunction() {
  const copyText = document.getElementById("myInput").textContent;
  const textArea = document.createElement('textarea');
  textArea.textContent = copyText;
  document.body.append(textArea);
  textArea.select();
  document.execCommand("copy");
  k2button.innerText = "Text copied";
    textArea.remove();
}
document.getElementById('k2button').addEventListener('click', copyFunction);
  </script>