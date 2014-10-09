/* 		OO PARSER AND BYTE-CODE GENERATOR FOR TINY PL
 
Grammar for TinyPL (using EBNF notation) is as follows:

 program ->  decls stmts end
 decls   ->  int idlist ;
 idlist  ->  id { , id } 
 stmts   ->  stmt [ stmts ]
 cmpdstmt->  '{' stmts '}'
 stmt    ->  assign | cond | loop
 assign  ->  id = expr ;
 cond    ->  if '(' rexp ')' cmpdstmt [ else cmpdstmt ]
 loop    ->  while '(' rexp ')' cmpdstmt  
 rexp    ->  expr (< | > | =) expr
 expr    ->  term   [ (+ | -) expr ]
 term    ->  factor [ (* | /) term ]
 factor  ->  int_lit | id | '(' expr ')'
 
Lexical:   id is a single character; 
	      int_lit is an unsigned integer;
		 equality operator is =, not ==

Sample Program: Factorial
 
int n, i, f;
n = 4;
i = 1;
f = 1;
while (i < n) {
  i = i + 1;
  f= f * i;
}
end

Sample Program:  GCD
   
int x, y;
x = 121;
y = 132;
while (x != y) {
  if (x > y) 
       { x = x - y; }
  else { y = y - x; }
}
end

 */

/*
 Sample Program - simple arithmetic with no paranthesis
int a, b, c , d;

a = 4; b = 5; c = 6;

d = 4 * 6 + 3;

end
 */

import java.util.HashMap;
import java.util.ArrayList;


public class Parser {
	static HashMap<Character, Integer> id_map = new HashMap<Character, Integer>();
	static int id = 0;
	public static void main(String[] args)  {
		System.out.println("Enter program and terminate with 'end'!\n");
		Code.setCodes();
		Lexer.lex();
		Program p = new Program();
		System.out.println("\n ----> Here is the output bytecode");
		Code.output();
		System.out.println("program ended!");
	}
}

class Program {
	public Program(){
	 if(Lexer.nextToken != Token.KEY_END){
		 Decls d = new Decls();
		 Stmts s = new Stmts();	
		 Program p = new Program();
	 }
	 else{
		 Code.out.add("return");
	 }
	}	 
}

class Decls {
	public Decls(){
		if(Lexer.nextToken == Token.KEY_INT){
			Lexer.lex();
			Idlist idl = new Idlist();	
		}
	}	 
}

class Idlist {
	public Idlist(){
		if(Lexer.nextToken == Token.COMMA){
			Lexer.lex();
			Idlist idl = new Idlist();
		}		
		//exit when you see the semicolon
		else if(Lexer.nextToken == Token.SEMICOLON){
			Lexer.lex();
			return;
		}
		//risky. bad default
		else if(Lexer.ident!=' '){
			Parser.id_map.put(Lexer.ident, Parser.id++);
			Lexer.lex();
			Idlist idl = new Idlist();
		}
	}	 
}

class Stmt {
	public Stmt(){
//		if(Lexer.nextToken==Token.KEY_WHILE){
//			Loop l = new Loop();
//			Stmt st = new Stmt();
//		}		
//		else if(Lexer.nextToken == Token.KEY_IF){
//			Cond c = new Cond();
//			Stmt st = new Stmt();
//		}
		Assign a = new Assign();

	}	 
} 

class Stmts {
	public Stmts(){
		Stmt s = new Stmt();
	}	 
}

class Assign {
	public Assign(){
		Lexer.lex();
		char c = Lexer.ident;
		if(Parser.id_map.containsKey(c)){
			if(Lexer.nextToken == Token.ASSIGN_OP){
				Expr E = new Expr();
			}
//				Lexer.lex();
//				if(Parser.id_map.containsKey(Lexer.ident)){
//					Expr E = new Expr();
//				}
		}
	}
}

class Cond {
	 
}

class Loop {
	 
}

class Cmpdstmt {
	 
}

class Rexpr {
	 
}

class Expr {
	public Expr(){
		Term t = new Term();		
		if(Lexer.nextToken == Token.ADD_OP || Lexer.nextToken == Token.SUB_OP){
			int op = Lexer.nextToken;
			Lexer.lex();
			Expr e = new Expr();
			Code.out.add(Code.getCode(op));	
		}
	}	 
}

class Term {  
	public Term(){
		Factor f = new Factor();
		if(Lexer.nextToken == Token.MULT_OP || Lexer.nextToken == Token.DIV_OP){	
			int op = Lexer.nextToken;
			Lexer.lex();
			Term t = new Term();
			Code.out.add(Code.getCode(op));				
		}
	}
	 
}

class Factor {  
	 public Factor(){
		char c = Lexer.ident;
		Lexer.lex();
		if(Lexer.nextToken == Token.INT_LIT){
			//perform some register arithmetic here
			Lexer.lex();
			//for assignment only
			if(Lexer.nextToken == Token.SEMICOLON){
				Code.out.add("iconst_" + Lexer.intValue);
				Code.out.add("istore_" + Parser.id_map.get(c));
				Lexer.lex();
			}
			
			//load the integer to the register for further operations
			else{
				Code.out.add("bipush " + Lexer.intValue);
				Lexer.lex();
			}
		}		
		else if(Lexer.nextToken == Token.LEFT_PAREN){
			Lexer.lex();
			Expr ex = new Expr();
			Lexer.lex();
		}
		else if(Parser.id_map.containsKey(c)){
			//perform some arithmetic here using the register
			Lexer.lex();
			Code.out.add("iload_" + Parser.id_map.get(c));
		}	
	 }
}

class Code {
	static HashMap<Integer, String> opcode= new HashMap<Integer, String>();
	public static void setCodes(){
		opcode.put(Token.MULT_OP, "imult");
		opcode.put(Token.SUB_OP, "isub");
		opcode.put(Token.ADD_OP, "iadd");
		opcode.put(Token.DIV_OP, "idiv");
	}
	
	public static String getCode(int a){
		return opcode.get(a);
	}

	static ArrayList<String> out = new ArrayList<String>();
	public static void output(){
		for(int o = 0; o<out.size(); o++){
			System.out.println(o + " : " + out.get(o));
		}	
		out = new ArrayList<String>();
	}	 
}


