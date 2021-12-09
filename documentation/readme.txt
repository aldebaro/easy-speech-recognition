Aldebaro. Dec. 2021.
Recall that ufpaspeech was called Spock.

https://www.youtube.com/watch?v=S6e6T4TnlOY&ab_channel=AldebaroKlautau

1) Find the jar files (e.g. at final_projects\AutomaticSpeechRecognitionAndSpeakerIdentification\ufpaspeech\jar):
05/07/2012  02:20 AM         3,497,277 dukov.jar
05/07/2012  02:20 AM         3,497,276 ufpaspeech.jar

2) Collect speech data for your isolated 

Let's suppose an example with 2 words: yes and no.

Considering the root as C:\Databases, one should create word directory yes:

C:\Databases\yes

and put there files with this word.

Similarly, create word directory no:

C:\Databases\no

The important thing is that below the root, each subdirectory is considered to be a new word. The software will search recursively the subdirectories of word directory (yes and no in this case) to build their two HMM models. So, one could eventually use subdirectories inside a word directory. For example:

C:\Databases\no\speaker1

C:\Databases\no\speaker2

could be used to divide the files of "no" according to the speaker. 

At this point, the software is able to automatically create a file TBL with a table of labels. Each subdirectory of root (word directory) is 

3) Choose configuration files for the frontend (FCN extension) and HMM topology (HCN extension) from examples in the folder called "config"

4) Provide the information about folders and then run the simulation.