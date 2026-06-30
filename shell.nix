{ pkgs ? import <nixpkgs> {} }:

pkgs.mkShell {
  buildInputs = [
    pkgs.vim
    pkgs.zellij
    pkgs.zulu21
  ];

  shellHook = ''
    export JAVA_HOME="${pkgs.zulu21}"
    export PATH="$JAVA_HOME/bin:$PATH"
    echo "java 21, vim and zellij are now available";
    echo "JAVA_HOME=$JAVA_HOME"
  '';

  REGISTRY_USERNAME = "igor";
}
