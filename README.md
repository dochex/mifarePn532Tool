This program is working with a PN532 module connected in UART to the PC. It is written for Mifare classic 1K Tag.
I have'nt found any java program for this use, only program for android with their reader module, C with arduino and libnfc, or C#.

The program use a windows compiled version of libnfc v1.80 that i have found at https://github.com/xavave/MifareOneTool-English/tree/master/MifareOneTool/nfc-bin64. I tried to compile it myself, but it was too tedious. 

The communication use JSerialComm, so import it and put it in build path.

To use it:
- first click on the 'connect' button : it will detect and awake the PN532 module
- then put a tag on the module and click on the button 'getUid' : it will detect the tag

***** Reading the Tag ****

To read the content of the tag there are three way depending on if it is a new tag or not
- For a new Tag, click on 'read Blocks with default Keys'
- for other tags, if the keys are known go to the bottom and fill the field keyA and KeyB then click on 'read with KeyA and KeyB'. So we can see the content of the Tag that the keys can decrypt.
- If the Keys are not known then we can click on the button 'search Keys with' and then choose a file.
It will open the exe 'mfoc-hardnested' of libnfc (in the nfc-bin64 dir) who will test keys in the choosen file; It can take a long time and each test is seing as a '.' until it find a bit of one key.
If the keys are found, then the Tag will be read, and a file with the bin content of the tag will be save in the directory 'nfc-64/sauvegardes' with the name 'saved.mfd'
This file can be see with the button 'ouvrir un dump'. The Keys found can be manually copied from this file and paste manually in the 'nfc-64/keysFound.keys' file.

We can also try to search a key with the 'mfcuk' button, but i never succeed with mfcuk.

NB: The KeyA cannot never be reading with the access conditions, so when we read it it always appear as '00 00 00 00 00 00'. But when reading whith the 'mfoc-hardnested' way we can see it.

***** Writing to the Tag ****

Before writing, it is important to know the access conditions of the sector in which we want to write. We have to kow the key who can read or not the blocks of the sector.
So for a new tag it is the default keys but for the other it is necessary to read the tag and verify the access conditions.
We have two way to write :
- we want to write one block at a time : we can do it with the button 'Write to Block' at the bottom. Before we must enter the 16 bytes of the block, the number of the block and the good key.
- we want to clone an entire dump to a tag: click on the button 'Write a dump' and look at the dialog box
    + The best way is to change first the access condition and put them for each sector to the default (FF0780) which are
        * Block 0: R:A|B W:A|B I:A|B D:A|B
        * Block 1: R:A|B W:A|B I:A|B D:A|B       //      for the blocks, Key A and B can write
        * Block 2: R:A|B W:A|B I:A|B D:A|B
        * Trailer: KeyA (R:- W:A) Access bits (R:A W:A) KeyB (R:A W:A)  //  for the block4 or trailer only KeyA can write
    + the program do it in two pass : first create the trailer of each sector with the keys of the dump and the access conditions choosen and write them with the keys of the tag
    + in a second pass it will write the block (less the trailers) with the keys who have been written on the tag
    + if it is a new tag the default key must be use for the first pass
    + if it is a tag that has already been written, we must have found the keys and put them in the dialog box
 
***** Testing access conditions ****

 At the bottom there is another button who can test the access condition. Put the three bytes and click
 The program only decode. If you want to encode you can go to this webside : https://slebe.dev/mifarecalc/

 ![Optional Text](https://github.com/dochex/mifarePn532Tool/blob/main/documentation/program.PNG)
