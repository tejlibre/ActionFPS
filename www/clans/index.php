<?php
require_once("../render.inc.php");
require("../render_game.inc.php");

$html = file_get_contents("http://woop.ac:81/html/clans/");
?>
    <article id="questions">
        <?php echo $html ?>
    </article>
<?php echo $foot;