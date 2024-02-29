# Text-to-RDF-Graph-Generator
- Java app that converts a text into RDF/XML format triples and generate a graph.
- The app utilizes StanfordCoreNLP, coreference resolution is solved however it doesn't provide accurate results always.
- Paragraph is divided into sentences, and each sentence is processed to extract Subject-Verb-Object.
- Coordinating Conjunctions like 'and' is solved i.e if we have two subjects separated by, and they are considered as two separate resources.
- The app will not generate accurate results on complex sentences, future work will cover this feature.
