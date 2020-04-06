<?php
header('Access-Control-Allow-Origin: *'); ?>
<?php
ini_set('memory_limit', '1024M') ?>
<?php
include 'SpellCorrector.php';

// make sure browsers see this page as utf-8 encoded HTML

header('Content-Type: text/html; charset=utf-8');
$limit = 10;
$query = isset($_REQUEST['q']) ? $_REQUEST['q'] : false;
$results = false;

if ($query)
	{

	// The Apache Solr Client library should be on the include path
	// which is usually most easily accomplished by placing in the
	// same directory as this script ( . or current directory is a default
	// php include path entry in the php.ini)

	require_once ('Apache/Solr/Service.php');

	// create a new solr service instance - host, port, and corename
	// path (all defaults in this example)

	$solr = new Apache_Solr_Service('localhost', 8983, '/solr/assignment/');

	// if magic quotes is enabled then stripslashes will be needed

	if (get_magic_quotes_gpc() == 1)
		{
		$query = stripslashes($query);
		}

	// in production code you'll always want to use a try /catch for any
	// possible exceptions emitted by searching (i.e. connection
	// problems or a query parsing error)

	try
		{
		if ($_GET['search_algo'] == "lucene")
			{
			$results = $solr->search($query, 0, $limit);
			}
		  else
			{
			$additionalParameters = array(
				'sort' => 'pageRankFile.txt desc'
			);
			$results = $solr->search($query, 0, $limit, $additionalParameters);
			}
		}

	catch(Exception $e)
		{

		// in production you'd probably log or email this error to an admin
		// and then show a special message to the user but for this example
		// we're going to show the full exception

		die("<html><head><title>SEARCH EXCEPTION</title><body><pre>{$e->__toString() }</pre></body></html>");
		}
	}

?>
<html>
<head>
<link rel="stylesheet" href="//code.jquery.com/ui/1.12.1/themes/base/jquery-ui.css">
<title>Search Engine Comparison Using Solr</title>
</head>
<body>
	<script src="https://code.jquery.com/jquery-1.12.4.js"></script>
  <script src="https://code.jquery.com/ui/1.12.1/jquery-ui.js"></script>
  <script>
  $( function() {
    $( "#q" ).autocomplete({
      source: function( request, response ) {
			var queryTerm = $("#q").val();
			var splits = queryTerm.split(" ");
			var previousTerm = "";
			var currTerm = queryTerm;
			if(splits.length > 1) {
				currTerm =  splits[splits.length - 1];
				var lastIndex = queryTerm.lastIndexOf(" ");
				previousTerm = queryTerm.substring(0, lastIndex);
			}
			var url = "http:\/\/localhost:8983/solr/assignment/suggest?q=" + currTerm + "&wt=json";
			console.log(url);
        $.ajax( {
          url: url,
					crossDomain: true,
          dataType: "jsonp",
					jsonp : 'json.wrf',
          success: function( data ) {
						var suggestionArr = JSON.parse(JSON.stringify(data)).suggest.suggest[currTerm].suggestions;
						results = []
						for (var i =0; i < suggestionArr.length; i++) {
							if(previousTerm != "") {
								results[i] = previousTerm + " "  + suggestionArr[i].term;
						} else {
								results[i] = suggestionArr[i].term;
						}
						}
						console.log(results);
            response( results );
          }
        } );
      },
      minLength: 1,
    } );
  } );
  </script>
<style>
table, th, td {
	padding:10px 10px 10px 10px;
}
</style>
<center><h2> Search Engine Comparison Using Solr </h2></center>
<form accept-charset="utf-8" method="get">
<center>
<label for="q">Search Term:</label>
<input id="q" name="q" type="text" value="<?php
echo htmlspecialchars($query, ENT_QUOTES, 'utf-8'); ?>" required/>
<?php

if ($query)
	{
	$terms = explode(" ", $query);
	$corrected_term = "";
	for ($i = 0; $i < sizeof($terms); $i++)
		{
		$correction = SpellCorrector::correct($terms[$i]);
		if ($i == 0) $corrected_term = $corrected_term . $correction;
		  else $corrected_term = $corrected_term . ' ' . $correction;
		}

	if ($corrected_term != "" && $corrected_term != strtolower($query))
		{
		echo "<br /><div style='padding-top:10px'>Did you mean: <a href='?q=" . $corrected_term . "&search_algo=" . $_REQUEST['search_algo'] . "'>" . $corrected_term . "</a><br /></div>";
		}
	}

?>
<br />
<input type="radio" name="search_algo" value="lucene"
<?php

if (isset($_REQUEST['search_algo']))
	{
	if ($_REQUEST['search_algo'] == 'lucene')
		{
		echo 'checked="checked"';
		}
	}
  else
	{
	echo ' checked';
	}

?> > Lucene
<input type="radio" name="search_algo" value="page_rank"<?php

if (isset($_REQUEST['search_algo']) && $_REQUEST['search_algo'] == 'page_rank')
	{
	echo 'checked="checked"';
	} ?>> Page Rank


      <br/>

<br /><input type="submit"/>
</center>
</form>
<?php

// display results

if ($results)
	{
	$total = (int)$results->response->numFound;
	$start = min(1, $total);
	$end = min($limit, $total);
	if ($total == 0)
		{
		echo "<center> No Results Found</center>";
		}
	  else
		{
		echo "<div>Results ";
		echo $start . " - ";
		echo $end . " of ";
		echo $total . ":";
		echo "</div>";

		// iterate result documents

		$data = [];
		$csvFile = file('/home/mukund/ir/assignment4/Reuters/URLtoHTML_reuters_news.csv');
		$data = [];
		foreach($csvFile as $line)
			{
			$data[] = str_getcsv($line);
			}

		echo "<center> <table bor>";
		$counter = 1;
		foreach($results->response->docs as $doc)
			{
			echo "<tr>";
			echo "<td style='padding-right:40px;'>" . $counter . "</td>";
			echo "<td>";
			$tit = $doc->title;
			if (is_array($tit)) {
			      $title = $tit[0];
			} else {
			       $title = $tit;
		            }
			$url = $doc->og_url;
			$id = $doc->id;
			$description = $doc->og_description;
			if ($description == "" || $description == null)
				{
				$description = "N/A";
				}

			if ($title == "" || $title == null)
				{
				$title = "N/A";
				}

			if ($url == "" || $url == null)
				{
				foreach($data as $row)
					{
					$existingURL = "/home/mukund/ir/assignment4/Reuters/reutersnews/" . $row[0];
					if ($id == $existingURL)
						{
						$url = $row[1];
						break;
						}
					}
				}

			$fileName = explode("/", $id);
			$content = file_get_contents("/home/mukund/ir/assignment4/Reuters/parsed/".$fileName[count($fileName)-1]);
			$separator = "\r\n";
			$line = strtok($content, $separator);
			$snippet = "";
			$query_terms = explode(" ", $query);
			$curr_max = 0;
			while ($line !== false)
				{
					$sentence = strtolower($line);
					$count = 0;
  				for($i = 0 ; $i < sizeof($query_terms); $i++)
  				{
		  			$query_term_lower = strtolower($query_terms[$i]);
		  			if(preg_match("/(^|\s)".$query_term_lower."($|\s|,|'s|’s)/",$sentence))
		  			{
		  				$count = $count+1;
		  			}
  				}
  				if(sizeof($query_terms)==$count)
	    		{
	    			$snippet = $line;
						break;
	    		}
	    		else if($count > $curr_max)
	    		{
						$curr_max = $count;
	    	    $snippet = $line;
	    		}
					$line = strtok($separator);

				}
			if(strlen($snippet) > 160) {
				$words = preg_split('/\s+/', $snippet);
				for($i = 0; $i < count($query_terms); $i++) {
					$pos = array_search(strtolower($query_terms[$i]), array_map('strtolower', $words));
					if($pos != false) {
						break;
					}
				}
				$left = $pos - 1;
				$right = $pos + 1;
				$charCount = strlen($words[$pos]);
				$finalSnippet = $words[$pos];
				$var = count($words);
				while($charCount <= 150 && ($left >=0 || $right <= count($words) - 1)) {
					if($left >= 0) {
						  $tempWord = $words[$left];
							$finalSnippet = $tempWord." " .$finalSnippet;
							$left--;
							$charCount = $charCount + strlen($tempWord) + 1;
					}
					if($right <= count($words) - 1) {
						$tempWord = $words[$right];
						$finalSnippet = $finalSnippet." " .$tempWord;
						$right++;
						$charCount = $charCount + strlen($tempWord) + 1;
					}
				}
				if($left>=0) {
					$finalSnippet = "...".$finalSnippet;
				}
				if($right <= count($words) - 1){
					$finalSnippet = $finalSnippet."...";
				}
				$snippet = $finalSnippet;
			}

			echo "Title : <a href = '$url' target='_blank'>$title</a></br>";
			echo "URL : <a href = '$url'  target='_blank'>$url</a></br>";
			echo "ID : $id</br>";
			if($snippet != "") {
				for($i = 0 ; $i < sizeof($query_terms); $i++) {
					$snippet = preg_replace('/(^|\s)'.preg_quote($query_terms[$i])."($|\s|,|'s|’s)/i", " <b>$0</b> ", $snippet);
				}
				echo "Snippet : <i>$snippet</i>";
			}
			echo "</td>";
			$counter = $counter + 1;
			}
		}
	}

?>
</body>
</html>
