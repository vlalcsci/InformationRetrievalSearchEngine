
<?php

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
<title>Search Engine Comparison Using Solr</title>
</head>
<body>
<style>
table, th, td {
  border: 1px solid black;
}
</style>
<center><h2> Search Engine Comparison Using Solr </h2></center>
<form accept-charset="utf-8" method="get">
<center>
<label for="q">Search Term:</label>
<input id="q" name="q" type="text" value="<?php echo htmlspecialchars($query, ENT_QUOTES, 'utf-8'); ?>" required/><br /><br />
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

		echo "<table bor>";
		$counter = 1;
		foreach($results->response->docs as $doc)
			{
			echo "<tr>";
			echo "<td style='padding-right:40px;'>" . $counter . "</td>";
			echo "<td>";
			$title = $doc->title;
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

			echo "Title : <a href = '$url' target='_blank'>$title</a></br>";
			echo "URL : <a href = '$url' target='_blank'>$url</a></br>";
			echo "ID : $id</br>";
			echo "Description : $description";
			echo "</td>";
			$counter = $counter + 1;
			}
		}
	}

?>
</body>
</html>
